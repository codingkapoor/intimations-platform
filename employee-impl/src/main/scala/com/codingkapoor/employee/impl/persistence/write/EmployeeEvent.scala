package com.codingkapoor.employee.impl.persistence.write

import java.time.{LocalDate, LocalDateTime}

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.model.{ContactInfo, Leaves, Location, Request}

object EmployeeEvent {
  val Tag: AggregateEventTag[EmployeeEvent] = AggregateEventTag[EmployeeEvent]
}

sealed trait EmployeeEvent extends AggregateEvent[EmployeeEvent] {
  def aggregateTag: AggregateEventTag[EmployeeEvent] = EmployeeEvent.Tag
}

case class EmployeeAdded(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                         contactInfo: ContactInfo, location: Location, leaves: Leaves) extends EmployeeEvent

object EmployeeAdded {
  implicit val format: Format[EmployeeAdded] = Json.format[EmployeeAdded]
}

case class EmployeeUpdated(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                         contactInfo: ContactInfo, location: Location, leaves: Leaves) extends EmployeeEvent

object EmployeeUpdated {
  implicit val format: Format[EmployeeUpdated] = Json.format[EmployeeUpdated]
}

case class EmployeeTerminated(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                              contactInfo: ContactInfo, location: Location, leaves: Leaves) extends EmployeeEvent

object EmployeeTerminated {
  implicit val format: Format[EmployeeTerminated] = Json.format[EmployeeTerminated]
}

case class EmployeeDeleted(id: Long) extends EmployeeEvent

object EmployeeDeleted {
  implicit val format: Format[EmployeeDeleted] = Json.format[EmployeeDeleted]
}

case class IntimationCreated(empId: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeEvent

object IntimationCreated {
  implicit val format: Format[IntimationCreated] = Json.format[IntimationCreated]
}

case class IntimationUpdated(empId: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeEvent

object IntimationUpdated {
  implicit val format: Format[IntimationUpdated] = Json.format[IntimationUpdated]
}

case class IntimationCancelled(empId: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeEvent

object IntimationCancelled {
  implicit val format: Format[IntimationCancelled] = Json.format[IntimationCancelled]
}
