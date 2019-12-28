package com.codingkapoor.passwordless.impl.core

import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.passwordless.api.PasswordlessService
import com.codingkapoor.passwordless.impl.service.{MailOTPService, PasswordlessServiceImpl}

abstract class PasswordlessApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[PasswordlessService](wire[PasswordlessServiceImpl])

  lazy val mailOTPService: MailOTPService = wire[MailOTPService]
  lazy val employeeService: EmployeeService = serviceClient.implement[EmployeeService]
}
