package com.codingkapoor.employee.api

import java.time.LocalDate
import play.api.libs.json._

case class EmployeeAddedEvent(id: String, name: String, gender: String, doj: LocalDate, pfn: String)

object EmployeeAddedEvent {
  implicit val format: Format[EmployeeAddedEvent] = Json.format[EmployeeAddedEvent]
}
