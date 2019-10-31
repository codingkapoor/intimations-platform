package com.codingkapoor.employee.persistence.write

import java.time.LocalDate

import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.model.{ContactInfo, IntimationReq, Leaves, Location}

case class EmployeeState(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                         contactInfo: ContactInfo, location: Location, leaves: Leaves, intimations: List[IntimationReq])

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
