package com.codingkapoor.employees.persistence.write

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class EmployeeState(id: String, name: String, gender: String, doj: LocalDate, pfn: String)

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
