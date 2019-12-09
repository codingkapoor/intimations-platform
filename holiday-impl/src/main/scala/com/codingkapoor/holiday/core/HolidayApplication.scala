package com.codingkapoor.holiday.core

import com.codingkapoor.holiday.api.HolidayService
import com.codingkapoor.holiday.repository.HolidayDao
import com.codingkapoor.holiday.service.HolidayServiceImpl
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HolidayApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  override lazy val lagomServer: LagomServer = serverFor[HolidayService](wire[HolidayServiceImpl])

  lazy val holidayDao: HolidayDao = wire[HolidayDao]
}
