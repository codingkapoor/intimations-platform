package com.codingkapoor.employee.api

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

sealed trait EmployeeEvent

object EmployeeEvent {
  implicit val format: Format[EmployeeEvent] = Json.format[EmployeeEvent]
}

case class EmployeeAdded(id: String, name: String, gender: String, doj: LocalDate, pfn: String) extends EmployeeEvent

object EmployeeAdded {
  implicit val format: Format[EmployeeAdded] = Json.format[EmployeeAdded]
}

case class EmployeeUpdated(id: String, name: String, gender: String, doj: LocalDate, pfn: String) extends EmployeeEvent

object EmployeeUpdated {
  implicit val format: Format[EmployeeUpdated] = Json.format[EmployeeUpdated]
}
