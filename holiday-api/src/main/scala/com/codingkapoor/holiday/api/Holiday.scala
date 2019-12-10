package com.codingkapoor.holiday.api

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

case class Holiday(date: LocalDate, occasion: String)

object Holiday {
  implicit val format: Format[Holiday] = Json.format[Holiday]
}

case class HolidayRes(id: Long, date: LocalDate, occasion: String)

object HolidayRes {
  implicit val format: Format[HolidayRes] = Json.format[HolidayRes]
}
