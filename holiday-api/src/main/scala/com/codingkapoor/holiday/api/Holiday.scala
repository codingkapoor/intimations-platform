package com.codingkapoor.holiday.api

import java.time.LocalDate
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import play.api.libs.json.{Format, Json}

case class Holiday(date: LocalDate, occasion: String)

object Holiday {
  implicit val format: Format[Holiday] = Json.format[Holiday]
}

case class HolidayRes(id: Long, date: LocalDate, occasion: String)

object HolidayRes {
  implicit val format: Format[HolidayRes] = Json.format[HolidayRes]
}

case class MonthYear(month: Int, year: Int)

object MonthYear {
  implicit val format: Format[MonthYear] = Json.format[MonthYear]

  private def serializer(monthYear: MonthYear): String = Json.stringify(Json.toJson(monthYear))

  private def deserializer(str: String): MonthYear = Json.fromJson(Json.parse(str)).get

  implicit val monthYearPathParamSerializer: PathParamSerializer[MonthYear] = {
    PathParamSerializer.required[MonthYear]("MonthYear")(deserializer)(serializer)
  }
}
