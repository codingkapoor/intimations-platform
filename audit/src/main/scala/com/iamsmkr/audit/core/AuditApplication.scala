package com.iamsmkr.audit.core

import com.iamsmkr.employee.api.EmployeeService
import com.iamsmkr.audit.services.{AuditService, AuditServiceImpl}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents

abstract class AuditApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with LagomKafkaClientComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[AuditService](wire[AuditServiceImpl])

  lazy val employeeService = serviceClient.implement[EmployeeService]
}
