package com.codingkapoor.passwordless.api

import akka.{Done, NotUsed}
import com.codingkapoor.passwordless.api.model.Tokens
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method

trait PasswordlessService extends Service {

  type OTP = Long
  type Refresh = String
  type JWT = String

  def createOTP(email: String): ServiceCall[NotUsed, Done]

  def createTokens(email: String): ServiceCall[OTP, Tokens]

  def createJWT(email: String): ServiceCall[Refresh, JWT]

  override def descriptor: Descriptor = {
    import Service._

    named("passwordless")
      .withCalls(
        restCall(Method.GET, "/api/passwordless/employees/:email/otp", createOTP _),
        restCall(Method.POST, "/api/passwordless/employees/:email/tokens", createTokens _),
        restCall(Method.POST, "/api/passwordless/employees/:email/jwt", createJWT _)
      )
      .withAutoAcl(true)
  }
}
