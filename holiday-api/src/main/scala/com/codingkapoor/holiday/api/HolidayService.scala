package com.codingkapoor.holiday.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

trait HolidayService extends Service {

  def addHoliday(): ServiceCall[Holiday, Done]

  def deleteHoliday(id: Long): ServiceCall[NotUsed, Done]

  def getHolidays: ServiceCall[NotUsed, Seq[Holiday]]

  override final def descriptor: Descriptor = {
    import Service._

    named("holiday")
      .withCalls(
        restCall(Method.POST, "/api/holidays", addHoliday _),
        restCall(Method.DELETE, "/api/holidays/:id", deleteHoliday _),
        restCall(Method.GET, "/api/holidays", getHolidays _),
      )
      .withAutoAcl(true)
  }
}
