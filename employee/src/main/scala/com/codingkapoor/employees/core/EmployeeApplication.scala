package com.codingkapoor.employees.core

import com.codingkapoor.employees.api.EmployeeServiceApi
import com.codingkapoor.employees.persistence.read.{EmployeeEventProcessor, EmployeeRepository}
import com.codingkapoor.employees.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employees.service.EmployeeService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.lightbend.lagom.scaladsl.persistence.cassandra.WriteSideCassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.ReadSideJdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class EmployeeApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with WriteSideCassandraPersistenceComponents
    with LagomKafkaComponents
    with ReadSideJdbcPersistenceComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[EmployeeServiceApi](wire[EmployeeService])
  override lazy val jsonSerializerRegistry = EmployeeSerializerRegistry

  lazy val employeeRepository = wire[EmployeeRepository]

  persistentEntityRegistry.register(wire[EmployeePersistenceEntity])
  readSide.register(wire[EmployeeEventProcessor])
}
