package com.codingkapoor.holiday.api

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

trait HolidayService extends Service with HolidayPathParamSerializer {

  def addHoliday(): ServiceCall[Holiday, Done]

  def deleteHoliday(date: LocalDate): ServiceCall[NotUsed, Done]

  def getHolidays(start: MonthYear, end: MonthYear): ServiceCall[NotUsed, Seq[Holiday]]

  override final def descriptor: Descriptor = {
    import Service._
    import MonthYear._

    named("holiday")
      .withCalls(
        restCall(Method.POST, "/api/holidays", addHoliday _),
        restCall(Method.DELETE, "/api/holidays/:date", deleteHoliday _),
        restCall(Method.GET, "/api/holidays?start&end", getHolidays _)
      )
      .withAutoAcl(true)
  }
}
