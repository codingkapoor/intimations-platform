package com.codingkapoor.holiday.impl.core

import com.codingkapoor.holiday.api.HolidayService
import com.codingkapoor.holiday.impl.repository.HolidayDao
import com.codingkapoor.holiday.impl.service.HolidayServiceImpl
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire._
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
}
