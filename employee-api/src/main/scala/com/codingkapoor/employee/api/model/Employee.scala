package com.codingkapoor.employee.api.model

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class Leaves(earned: Int = 0, sick: Int = 0)

object Leaves {
  implicit val format: Format[Leaves] = Json.using[Json.WithDefaultValues].format[Leaves]
}

case class Employee(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean = true, leaves: Leaves = Leaves())

object Employee {
  implicit val format: Format[Employee] = Json.using[Json.WithDefaultValues].format[Employee]
}
