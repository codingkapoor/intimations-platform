package com.codingkapoor.holiday.impl.core

import java.util

import com.codingkapoor.holiday.api.HolidayService
import com.codingkapoor.holiday.impl.repository.HolidayDao
import com.codingkapoor.holiday.impl.service.HolidayServiceImpl
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire._
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants.{AUTHORIZATION_HEADER, BEARER_HEADER_PREFIX}
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.lagom.jwt.JwtAuthenticatorHelper
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HolidayApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[HolidayService](wire[HolidayServiceImpl])
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = HolidaySerializerRegistry

  lazy val holidayDao: HolidayDao = wire[HolidayDao]

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
