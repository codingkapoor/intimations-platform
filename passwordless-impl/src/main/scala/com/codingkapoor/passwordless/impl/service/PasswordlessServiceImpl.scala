package com.codingkapoor.passwordless.impl.service

import akka.Done
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.api.model.Tokens
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class PasswordlessServiceImpl(employeeService: EmployeeService, mailOTPService: MailOTPService) extends PasswordlessService {
  import PasswordlessServiceImpl._

  override def createOTP(): ServiceCall[Email, Done] = ServiceCall { email =>
    employeeService.getEmployees(Some(email)).invoke().map { res =>
      if (res.nonEmpty && res.head.isActive) {
        val otp = generateOTP
        mailOTPService.sendOTP(email, otp)

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
  private def generateOTP: Int =  100000 + Random.nextInt(999999)
}
