package com.codingkapoor.employee.api

import java.time.LocalDate
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer._

trait EmployeePathParamSerializer {
  implicit val datePathParamSerializer: PathParamSerializer[LocalDate] = {
    def serializer(date: LocalDate): String = date.toString
    def deserializer(str: String): LocalDate = LocalDate.parse(str)

    required[LocalDate]("LocalDate")(deserializer)(serializer)
  }

}
