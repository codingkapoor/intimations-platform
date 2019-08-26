package com.codingkapoor.employee.core

import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.service.EmployeeServiceImpl
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

abstract class EmployeeApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[EmployeeService](wire[EmployeeServiceImpl])
  override lazy val jsonSerializerRegistry = EmployeeSerializerRegistry

  persistentEntityRegistry.register(wire[EmployeePersistenceEntity])
}
