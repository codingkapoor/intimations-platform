package com.codingkapoor.passwordless.impl.service

import java.time.LocalDateTime

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.duration._
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.api.model.Tokens
import com.codingkapoor.passwordless.impl.repository.otp.{OTPDao, OTPEntity}
import com.codingkapoor.passwordless.impl.repository.token.RefreshTokenDao

class PasswordlessServiceImpl(employeeService: EmployeeService, mailOTPService: MailOTPService, otpDao: OTPDao, refreshTokenDao: RefreshTokenDao) extends PasswordlessService {

  import PasswordlessServiceImpl._

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

  // TODO: Web API**, RSA Keys, App Conf, Persistence, Validations
  override def createTokens(email: String): ServiceCall[OTP, Tokens] = ServiceCall { otp =>
    // if the submitted otp is valid, create and reply jwt and refresh token

    Future.successful(Tokens(otp.toString, otp.toString))
  }

  override def createJWT(email: String): ServiceCall[Refresh, JWT] = ServiceCall { refresh =>
    // if the submitted refresh token is valid, create and reply with a new jwt
    Future.successful(refresh)
  }
}

object PasswordlessServiceImpl {
  private def generateOTP: Int = 100000 + Random.nextInt(999999)
}
