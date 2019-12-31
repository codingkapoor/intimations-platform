package com.codingkapoor.employee.impl.core

import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.impl.persistence.read.EmployeeEventProcessor
import com.codingkapoor.employee.impl.persistence.read.repository.employee.EmployeeDao
import com.codingkapoor.employee.impl.persistence.read.repository.intimation.IntimationDao
import com.codingkapoor.employee.impl.persistence.read.repository.request.RequestDao
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.service.EmployeeServiceImpl
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

  lazy val employeeRepository = wire[EmployeeDao]
  lazy val intimationRepository = wire[IntimationDao]
  lazy val requestRepository = wire[RequestDao]

  persistentEntityRegistry.register(wire[EmployeePersistenceEntity])
  readSide.register(wire[EmployeeEventProcessor])
}
