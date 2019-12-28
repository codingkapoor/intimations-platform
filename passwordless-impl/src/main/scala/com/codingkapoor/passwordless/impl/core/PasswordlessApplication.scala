package com.codingkapoor.passwordless.impl.core

import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.impl.repository.otp.OTPDao
import com.codingkapoor.passwordless.impl.service.{MailOTPService, PasswordlessServiceImpl}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.db.HikariCPComponents

abstract class PasswordlessApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[PasswordlessService](wire[PasswordlessServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = PasswordlessSerializerRegistry

  lazy val employeeService: EmployeeService = serviceClient.implement[EmployeeService]

  lazy val mailOTPService: MailOTPService = wire[MailOTPService]
  lazy val otpDao: OTPDao = wire[OTPDao]
}
