package com.codingkapoor.holiday.api

import java.time.LocalDate
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer._

trait HolidayPathParamSerializer {
  implicit val datePathParamSerializer: PathParamSerializer[LocalDate] = {
    def serializer(date: LocalDate): String = date.toString
    def deserializer(str: String): LocalDate = LocalDate.parse(str)

    required[LocalDate]("LocalDate")(deserializer)(serializer)
  }

}
