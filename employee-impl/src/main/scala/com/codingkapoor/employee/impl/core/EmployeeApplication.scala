package com.codingkapoor.employee.impl.core

import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.impl.persistence.read.EmployeeEventProcessor
import com.codingkapoor.employee.impl.persistence.read.repository.employee.EmployeeDao
import com.codingkapoor.employee.impl.persistence.read.repository.intimation.IntimationDao
import com.codingkapoor.employee.impl.persistence.read.repository.request.RequestDao
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.service.{CreditScheduler, EmployeeServiceImpl}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.WriteSideCassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.ReadSideSlickPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire._
import org.pac4j.http.client.direct.HeaderClient
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import org.pac4j.core.context.HttpConstants.{AUTHORIZATION_HEADER, BEARER_HEADER_PREFIX}
import org.pac4j.lagom.jwt.JwtAuthenticatorHelper
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile
import java.util

import org.pac4j.core.config.Config

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

  wire[CreditScheduler]

  persistentEntityRegistry.register(wire[EmployeePersistenceEntity])
  readSide.register(wire[EmployeeEventProcessor])

  lazy val jwtClient: HeaderClient = {
    val headerClient = new HeaderClient
    headerClient.setHeaderName(AUTHORIZATION_HEADER)
    headerClient.setPrefixHeader(BEARER_HEADER_PREFIX)
    headerClient.setAuthenticator(JwtAuthenticatorHelper.parse(config.getConfig("pac4j.lagom.jwt.authenticator")))
    headerClient.setAuthorizationGenerator((_: WebContext, profile: CommonProfile) => {
      if (profile.containsAttribute("roles")) profile.addRoles(profile.getAttribute("roles", classOf[util.Collection[String]]))
      profile
    })
    headerClient.setName("jwt_header")
    headerClient
  }

  lazy val serviceConfig: Config = {
    val config = new Config(jwtClient)
    config.getClients.setDefaultSecurityClients(jwtClient.getName)
    config
  }
}
