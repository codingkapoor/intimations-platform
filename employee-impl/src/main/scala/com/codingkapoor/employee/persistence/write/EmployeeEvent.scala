package com.codingkapoor.employee.persistence.write

import java.time.LocalDate

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.model.{IntimationReq, Leaves, Request}

object EmployeeEvent {
  val Tag: AggregateEventTag[EmployeeEvent] = AggregateEventTag[EmployeeEvent]
}

sealed trait EmployeeEvent extends AggregateEvent[EmployeeEvent] {
  def aggregateTag: AggregateEventTag[EmployeeEvent] = EmployeeEvent.Tag
}

case class EmployeeAdded(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, leaves: Leaves) extends EmployeeEvent

object EmployeeAdded {
  implicit val format: Format[EmployeeAdded] = Json.format[EmployeeAdded]
}

case class EmployeeTerminated(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, leaves: Leaves) extends EmployeeEvent

object EmployeeTerminated {
  implicit val format: Format[EmployeeTerminated] = Json.format[EmployeeTerminated]
}

case class EmployeeDeleted(id: String) extends EmployeeEvent

object EmployeeDeleted {
  implicit val format: Format[EmployeeDeleted] = Json.format[EmployeeDeleted]
}

case class IntimationCreated(empId: String, reason: String, requests: List[Request]) extends EmployeeEvent

object IntimationCreated {
  implicit val format: Format[IntimationCreated] = Json.format[IntimationCreated]
}
