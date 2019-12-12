package com.codingkapoor.holiday.api

import java.time.LocalDate
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer._
import play.api.libs.json.Json

import MonthYear._

trait HolidayPathParamSerializer {
  implicit val datePathParamSerializer: PathParamSerializer[LocalDate] = {
    def serializer(date: LocalDate): String = date.toString
    def deserializer(str: String): LocalDate = LocalDate.parse(str)

    required[LocalDate]("LocalDate")(deserializer)(serializer)
  }

  implicit val monthYearPathParamSerializer: PathParamSerializer[MonthYear] = {
    def serializer(monthYear: MonthYear): String = Json.stringify(Json.toJson(monthYear))
    def deserializer(str: String): MonthYear = Json.fromJson(Json.parse(str)).get

    required[MonthYear]("MonthYear")(deserializer)(serializer)
  }
}
