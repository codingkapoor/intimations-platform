package com.codingkapoor.passwordless.impl.service

import java.time.LocalDateTime
import akka.Done
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

class PasswordlessServiceImpl(employeeService: EmployeeService, mailOTPService: MailOTPService, otpDao: OTPDao) extends PasswordlessService {

  import PasswordlessServiceImpl._

  override def createOTP(): ServiceCall[Email, Done] = ServiceCall { email =>
    employeeService.getEmployees(Some(email)).invoke().map { res =>
      if (res.nonEmpty && res.head.isActive) {
        val emp = res.head

        otpDao.getOTP(emp.contactInfo.email).flatMap { res =>
          if (res.isDefined) Await.result(otpDao.deleteOTP(res.get.otp), 5.seconds)

          val otp = generateOTP
          otpDao.createOTP(OTPEntity(otp, emp.id, emp.contactInfo.email, emp.roles, LocalDateTime.now())).map { _ =>
            mailOTPService.sendOTP(email, otp)
          }
        }

        Done
      } else throw NotFound(s"No employee found with email id = $email")
    }
  }

  override def createTokens(): ServiceCall[OTP, Tokens] = ServiceCall { otp =>
    // if the submitted otp is valid, create and reply jwt and refresh token

    Future.successful(Tokens(otp.toString, otp.toString))
  }

  override def createJWT(): ServiceCall[Refresh, JWT] = ServiceCall { refresh =>
    // if the submitted refresh token is valid, create and reply with a new jwt
    Future.successful(refresh)
  }
}

object PasswordlessServiceImpl {
  private def generateOTP: Int = 100000 + Random.nextInt(999999)
}
