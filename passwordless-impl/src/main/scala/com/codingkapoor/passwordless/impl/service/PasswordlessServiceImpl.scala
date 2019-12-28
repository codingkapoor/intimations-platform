package com.codingkapoor.passwordless.impl.service

import java.time.LocalDateTime
import java.util.{Date, UUID}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.duration._
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
                              refreshTokenDao: RefreshTokenDao, config: Configuration) extends PasswordlessService {

  import PasswordlessServiceImpl._

  private val (accessRSAKey, refreshRSAKey) = getRSAKeys(config)

  override def createOTP(email: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    employeeService.getEmployees(Some(email)).invoke().map { res =>
      if (res.nonEmpty && res.head.isActive) {
        val emp = res.head

        otpDao.getOTP(email).flatMap { res =>
          if (res.isDefined) Await.result(otpDao.deleteOTP(res.get.otp), 5.seconds)

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

        if (LocalDateTime.now().isAfter(otpEntity.createdAt.plusMinutes(1)))
          throw BadRequest("OTP Expired")
        else {
          val accessToken = createToken(otpEntity.empId.toString, otpEntity.roles, accessRSAKey).serialize
          val refreshToken = createToken(otpEntity.empId.toString, otpEntity.roles, refreshRSAKey).serialize

          refreshTokenDao.addRefreshToken(RefreshTokenEntity(refreshToken, otpEntity.empId, otpEntity.email, LocalDateTime.now())).flatMap { _ =>
            otpDao.deleteOTP(otpEntity.otp).flatMap { _ =>
              Future.successful(Tokens(accessToken, refreshToken))
            }
          }
        }

      } else throw BadRequest("Invalid OTP")
    }
  }

  override def createJWT(email: String): ServiceCall[Refresh, JWT] = ServiceCall { refresh =>
    // if the submitted refresh token is valid, create and reply with a new jwt
    Future.successful(refresh)
  }
}

object PasswordlessServiceImpl {
  private def generateOTP: Int = 100000 + Random.nextInt(999999)

  private def getRSAKeys(config: Configuration) = {
    val accessRSAKey = config.getOptional[String]("authenticator.signatures.RS256.access")
    val refreshRSAKey = config.getOptional[String]("authenticator.signatures.RS256.refresh")

    if (accessRSAKey.isEmpty || refreshRSAKey.isEmpty)
      throw new Exception("JWT signature keys are missing from the configuration file.")

    (RSAKey.parse(accessRSAKey.get), RSAKey.parse(refreshRSAKey.get))
  }

  import collection.JavaConverters._

  @throws[JOSEException]
  private def createToken(subject: String, roles: List[Role], rsaJWK: RSAKey): SignedJWT = {
    def createPayload(): JWTClaimsSet = {
      val payload = new JWTClaimsSet.Builder()
        .subject(subject)
        .claim("roles", ImmutableList.copyOf(roles.map(_.toString).toIterable.asJava))
        .issueTime(new Date)
        .jwtID(UUID.randomUUID.toString)
        .build

      payload
    }

    val header = new JWSHeader(RS256)
    val payload = createPayload()

    val token = new SignedJWT(header, payload)
    token.sign(new RSASSASigner(rsaJWK))

    token
  }
}
