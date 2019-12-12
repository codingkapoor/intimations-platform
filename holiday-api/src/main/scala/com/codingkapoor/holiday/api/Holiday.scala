package com.codingkapoor.holiday.api

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

case class Holiday(date: LocalDate, occasion: String)

object Holiday {
  implicit val format: Format[Holiday] = Json.format[Holiday]
}

case class MonthYear(month: Int, year: Int)

object MonthYear {
  implicit val format: Format[MonthYear] = Json.format[MonthYear]
}
