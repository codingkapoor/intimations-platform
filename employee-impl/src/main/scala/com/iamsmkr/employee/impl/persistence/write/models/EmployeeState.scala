package com.iamsmkr.employee.impl.persistence.write.models

import java.time.LocalDate

import com.iamsmkr.employee.api.models.Role.Role
import com.iamsmkr.employee.api.models.{ContactInfo, Intimation, Leaves, Location, PrivilegedIntimation}
import play.api.libs.json.{Format, Json}

case class EmployeeState(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String, pfn: String, contactInfo: ContactInfo,
                         location: Location, leaves: Leaves, roles: List[Role], activeIntimationOpt: Option[Intimation], privilegedIntimationOpt: Option[PrivilegedIntimation], lastLeaves: Leaves)

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
