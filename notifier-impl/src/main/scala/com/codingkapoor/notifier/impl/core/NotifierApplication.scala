package com.codingkapoor.notifier.impl.core

import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.notifier.api.NotifierService
import com.codingkapoor.notifier.impl.repository.employee.EmployeeDao
import com.codingkapoor.notifier.impl.service.NotifierServiceImpl
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.db.HikariCPComponents

abstract class NotifierApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with SlickPersistenceComponents
    with HikariCPComponents
    with LagomKafkaClientComponents
    with AhcWSComponents {
  override lazy val lagomServer: LagomServer = serverFor[NotifierService](wire[NotifierServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = NotifierSerializerRegistry

  lazy val employeeService: EmployeeService = serviceClient.implement[EmployeeService]

  lazy val employeeDao: EmployeeDao = wire[EmployeeDao]
}
