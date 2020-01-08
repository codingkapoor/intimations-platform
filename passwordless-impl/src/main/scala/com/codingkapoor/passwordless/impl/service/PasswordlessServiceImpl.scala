package com.codingkapoor.passwordless.impl.service

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.{Date, UUID}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import com.google.common.collect.ImmutableList
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.{JWTClaimsSet, JWTParser, SignedJWT}
import com.nimbusds.jose.JWSAlgorithm.RS256
import play.api.Configuration
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.model.Role.Role
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.api.model.Tokens
import com.codingkapoor.passwordless.impl.repository.employee.EmployeeDao
import com.codingkapoor.passwordless.impl.repository.otp.{OTPDao, OTPEntity}
import com.codingkapoor.passwordless.impl.repository.token.{RefreshTokenDao, RefreshTokenEntity}
import net.minidev.json.JSONArray
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class PasswordlessServiceImpl(override val employeeService: EmployeeService, override val otpDao: OTPDao, override val refreshTokenDao: RefreshTokenDao,
                              override val employeeDao: EmployeeDao, mailOTPService: MailOTPService, implicit val config: Configuration)
  extends PasswordlessService with EmployeeKafkaEventHandler {

  import PasswordlessServiceImpl._

  override val logger = LoggerFactory.getLogger(classOf[PasswordlessServiceImpl])

  override def createOTP(email: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    employeeDao.getEmployees(Some(email)).map { res =>
      if (res.nonEmpty && res.head.isActive) {
        val emp = res.head

        otpDao.deleteOTP(email).flatMap { _ =>
          val otp = generateOTP
          otpDao.createOTP(OTPEntity(otp, emp.id, email, emp.roles, ZonedDateTime.now(ZoneOffset.UTC))).map { _ =>
            mailOTPService.sendOTP(email, otp)
          }
        }

        Done
      } else throw NotFound(s"No employee found with email id = $email")
    }
  }

  override def createTokens(email: String): ServiceCall[OTP, Tokens] = ServiceCall { otp =>
    otpDao.getOTP(email).flatMap { res =>
      if (res.isDefined) {
        val otpEntity = res.get
        val empId = otpEntity.empId
        val roles = otpEntity.roles

        if (otp != otpEntity.otp)
          throw BadRequest("Invalid OTP")

        if (ZonedDateTime.now(ZoneOffset.UTC).isAfter(otpEntity.createdAt.plusMinutes(config.getOptional[Long]("expiries.otp").getOrElse(5))))
          otpDao.deleteOTP(email).map(throw BadRequest("OTP Expired"))
        else {
          val accessToken = createToken(empId, roles, ACCESS).serialize
          val refreshToken = createToken(empId, roles, REFRESH).serialize

          refreshTokenDao.deleteRefreshToken(email).flatMap { _ =>
            refreshTokenDao.addRefreshToken(RefreshTokenEntity(refreshToken, empId, email, ZonedDateTime.now(ZoneOffset.UTC))).flatMap { _ =>
              otpDao.deleteOTP(email).map { _ =>
                Tokens(accessToken, refreshToken)
              }
            }
          }
        }

      } else throw BadRequest("Invalid OTP")
    }
  }

  override def createJWT(email: String): ServiceCall[Refresh, JWT] = ServiceCall { refresh =>
    val payload = JWTParser.parse(refresh).getJWTClaimsSet
    val subject = payload.getSubject.toLong
    val roles = Json.parse(payload.getClaim("roles").asInstanceOf[JSONArray].toJSONString).as[List[Role]]

    refreshTokenDao.getRefreshToken(email).map { res =>
      if (res.isDefined) {
        if (refresh != res.get.refreshToken)
          throw BadRequest("Invalid Refresh Token")

        if (payload.getExpirationTime.before(new Date()))
          refreshTokenDao.deleteRefreshToken(email).map(throw BadRequest("Refresh Token Expired"))

        createToken(subject, roles, ACCESS).serialize
      } else throw BadRequest("Invalid Refresh Token")
    }
  }
}

object PasswordlessServiceImpl {

  private final val ACCESS = "Access"
  private final val REFRESH = "Refresh"

  private def generateOTP: Int = 100000 + Random.nextInt(899999)

  import collection.JavaConverters._

  @throws[JOSEException]
  private def createToken(subject: Long, roles: List[Role], tokenType: String)(implicit config: Configuration): SignedJWT = {
    def getRSAKeys(tokenType: String)(implicit config: Configuration): RSAKey = {
      val rsaKey = tokenType match {
        case ACCESS => config.getOptional[String]("authenticator.signatures.RS256.access")
        case REFRESH => config.getOptional[String]("authenticator.signatures.RS256.refresh")
        case _ => throw new Exception("Token type not recognized.")
      }

      if (rsaKey.isEmpty)
        throw new Exception("JWT signature keys are missing from the configuration file.")

      RSAKey.parse(rsaKey.get)
    }

    def createPayload(): JWTClaimsSet = {
      val now = ZonedDateTime.now(ZoneOffset.UTC)
      val issuedAt = Date.from(now.toInstant)
      val expiresAt = tokenType match {
        case ACCESS =>
          Date.from(now.plusMinutes(config.getOptional[Long]("expiries.tokens.access").getOrElse(5)).toInstant)
        case REFRESH =>
          Date.from(now.plusMonths(config.getOptional[Long]("expiries.tokens.refresh").getOrElse(12)).toInstant)
        case _ => throw new Exception("Token type not recognized.")
      }

      val payload = new JWTClaimsSet.Builder()
        .subject(subject.toString)
        .claim("roles", ImmutableList.copyOf(roles.map(_.toString).toIterable.asJava))
        .claim("type", tokenType)
        .issueTime(issuedAt)
        .expirationTime(expiresAt)
        .jwtID(UUID.randomUUID.toString)
        .build

      payload
    }

    val header = new JWSHeader(RS256)
    val payload = createPayload()

    val token = new SignedJWT(header, payload)
    token.sign(new RSASSASigner(getRSAKeys(tokenType)))

    token
  }
}
