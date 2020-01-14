package com.codingkapoor.employee.impl.persistence.write

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

import com.codingkapoor.employee.api.model.Role.Role
import com.codingkapoor.employee.api.model.{ContactInfo, Intimation, Leaves, Location}

case class EmployeeState(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                         contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role], intimations: List[Intimation], lastLeaves: Leaves)

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
