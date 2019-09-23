package com.codingkapoor.employee.persistence.write

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

import com.codingkapoor.employee.api.model.{IntimationReq, Leaves}

case class EmployeeState(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean,
                         leaves: Leaves, intimations: List[IntimationReq])

object EmployeeState {
  implicit val format: Format[EmployeeState] = Json.format[EmployeeState]
}
