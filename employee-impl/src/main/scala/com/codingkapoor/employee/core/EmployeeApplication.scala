package com.codingkapoor.employee.core

import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.persistence.read.EmployeeEventProcessor
import com.codingkapoor.employee.persistence.read.dao.employee.EmployeeRepository
import com.codingkapoor.employee.persistence.read.dao.intimation.IntimationRepository
import com.codingkapoor.employee.persistence.read.dao.request.RequestRepository
import com.codingkapoor.employee.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.service.EmployeeServiceImpl
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.WriteSideCassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.ReadSideSlickPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class EmployeeApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with LagomKafkaComponents
    with ReadSideSlickPersistenceComponents
    with WriteSideCassandraPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[EmployeeService](wire[EmployeeServiceImpl])
  override lazy val jsonSerializerRegistry = EmployeeSerializerRegistry

  lazy val employeeRepository = wire[EmployeeRepository]
  lazy val intimationRepository = wire[IntimationRepository]
  lazy val requestRepository = wire[RequestRepository]

  persistentEntityRegistry.register(wire[EmployeePersistenceEntity])
  readSide.register(wire[EmployeeEventProcessor])
}
