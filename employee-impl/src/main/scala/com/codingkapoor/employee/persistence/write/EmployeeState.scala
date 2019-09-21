package com.codingkapoor.employee.persistence.write

import java.time.LocalDate
import com.codingkapoor.employee.api.Leaves
import play.api.libs.json.{Format, Json}

case class EmployeeState(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, leaves: Leaves)

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
