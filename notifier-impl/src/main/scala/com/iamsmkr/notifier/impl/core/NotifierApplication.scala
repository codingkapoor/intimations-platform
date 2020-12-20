package com.iamsmkr.notifier.impl.core

import java.util

import com.iamsmkr.common.Mailer
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import play.api.libs.ws.ahc.AhcWSComponents
import com.iamsmkr.employee.api.EmployeeService
import com.iamsmkr.notifier.api.NotifierService
import com.iamsmkr.notifier.impl.repositories.employee.EmployeeDao
import com.iamsmkr.notifier.impl.services.{MailNotifier, NotifierServiceImpl, PushNotifier}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants.{AUTHORIZATION_HEADER, BEARER_HEADER_PREFIX}
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.lagom.jwt.JwtAuthenticatorHelper
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

  lazy val mailNotifier: MailNotifier = wire[MailNotifier]
  lazy val pushNotifier: PushNotifier = wire[PushNotifier]
  lazy val mailer: Mailer = wire[Mailer]

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
