package com.codingkapoor.passwordless.api

import akka.Done
import com.codingkapoor.passwordless.api.model.Tokens
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method

trait PasswordlessService extends Service {

  type Email = String
  type OTP = Long
  type Refresh = String
  type JWT = String

  def createOTP(): ServiceCall[Email, Done]

  // TODO: Better accept both email id and otp to create tokens otherwise people can exchange otps to login
  def createTokens(): ServiceCall[OTP, Tokens]

  def createJWT(): ServiceCall[Refresh, JWT]

  override def descriptor: Descriptor = {
    import Service._

    named("passwordless")
      .withCalls(
        restCall(Method.POST, "/api/passwordless/otp", createOTP _),
        restCall(Method.POST, "/api/passwordless/tokens", createTokens _),
        restCall(Method.POST, "/api/passwordless/jwt", createJWT _)
      )
  }
}
