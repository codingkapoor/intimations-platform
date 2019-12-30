package com.codingkapoor.passwordless.impl.service

import java.time.{LocalDateTime, ZoneId}
import java.util.{Date, UUID}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import com.google.common.collect.ImmutableList
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.JWSAlgorithm.RS256
import play.api.Configuration
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.model.Role.Role
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.api.model.Tokens
import com.codingkapoor.passwordless.impl.repository.otp.{OTPDao, OTPEntity}
import com.codingkapoor.passwordless.impl.repository.token.{RefreshTokenDao, RefreshTokenEntity}

class PasswordlessServiceImpl(employeeService: EmployeeService, mailOTPService: MailOTPService, otpDao: OTPDao,
                              refreshTokenDao: RefreshTokenDao, implicit val config: Configuration) extends PasswordlessService {

  import PasswordlessServiceImpl._

  override def createOTP(email: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    employeeService.getEmployees(Some(email)).invoke().map { res =>
      if (res.nonEmpty && res.head.isActive) {
        val emp = res.head

        otpDao.deleteOTP(email).flatMap { _ =>
          val otp = generateOTP
          otpDao.createOTP(OTPEntity(otp, emp.id, email, emp.roles, LocalDateTime.now())).map { _ =>
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

        if (LocalDateTime.now().isAfter(otpEntity.createdAt.plusMinutes(1)))
          otpDao.deleteOTP(email).map(throw BadRequest("OTP Expired"))
        else {
          val accessToken = createToken(empId.toString, roles, ACCESS).serialize
          val refreshToken = createToken(empId.toString, roles, REFRESH).serialize

          refreshTokenDao.deleteRefreshToken(empId).flatMap { _ =>
            refreshTokenDao.addRefreshToken(RefreshTokenEntity(refreshToken, empId, email, LocalDateTime.now())).flatMap { _ =>
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
    // if the submitted refresh token is valid, create and reply with a new jwt

    // check if refresh token is present in refresh_tokens table against the provided email id
    // if not found, reply with "Invalid Refresh Token" error response
    // else validdate if refresh token has not already expired
    // if expired then delete the refresh token entry from the table and reply with "Refresh Token Expired" error response
    // else create a new signed access token and reply the same to the client, use the claims from the refresh token itself to create the access token

    Future.successful(refresh)
  }
}

object PasswordlessServiceImpl {

  private final val ACCESS = "ACCESS"
  private final val REFRESH = "REFRESH"

  private def generateOTP: Int = 100000 + Random.nextInt(999999)

  import collection.JavaConverters._

  // TODO: See if can use pac4j to read jwk objects from the config file

  @throws[JOSEException]
  private def createToken(subject: String, roles: List[Role], tokenType: String)(implicit config: Configuration): SignedJWT = {
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
      val now = LocalDateTime.now()
      val issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant)
      val expiresAt = tokenType match {
        case ACCESS => Date.from(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant)
        case REFRESH => Date.from(now.plusMonths(12).atZone(ZoneId.systemDefault()).toInstant)
        case _ => throw new Exception("Token type not recognized.")
      }

      val payload = new JWTClaimsSet.Builder()
        .subject(subject)
        .claim("roles", ImmutableList.copyOf(roles.map(_.toString).toIterable.asJava))
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
