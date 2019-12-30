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

  // TODO: Add expiry to access and refresh tokens
  override def createTokens(email: String): ServiceCall[OTP, Tokens] = ServiceCall { otp =>
    otpDao.getOTP(email).flatMap { res =>
      if (res.isDefined) {
        val otpEntity = res.get
        val empId = otpEntity.empId
        val roles = otpEntity.roles

        // TODO: Delete the entry from the db
        if (LocalDateTime.now().isAfter(otpEntity.createdAt.plusMinutes(1)))
          throw BadRequest("OTP Expired")
        else {
          val accessToken = createToken(empId.toString, roles, accessRSAKey).serialize
          val refreshToken = createToken(empId.toString, roles, refreshRSAKey).serialize

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
  private def generateOTP: Int = 100000 + Random.nextInt(999999)

  // TODO: See if can use pac4j to read jwk objects from the config file
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
