package com.codingkapoor.employee.impl.persistence.write.models

import java.time.{LocalDate, LocalDateTime}

import com.codingkapoor.employee.api.models.PrerogativeIntimationType.PrerogativeIntimationType
import com.codingkapoor.employee.api.models.Role.Role
import com.codingkapoor.employee.api.models.{ContactInfo, Leaves, Location, Request}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import play.api.libs.json.{Format, Json}

object EmployeeEvent {
  val Tag: AggregateEventTag[EmployeeEvent] = AggregateEventTag[EmployeeEvent]
}

sealed trait EmployeeEvent extends AggregateEvent[EmployeeEvent] {
  def aggregateTag: AggregateEventTag[EmployeeEvent] = EmployeeEvent.Tag
}

case class EmployeeAdded(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String, pfn: String,
                         contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeEvent

object EmployeeAdded {
  implicit val format: Format[EmployeeAdded] = Json.format[EmployeeAdded]
}

case class EmployeeUpdated(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String, pfn: String,
                           contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeEvent

object EmployeeUpdated {
  implicit val format: Format[EmployeeUpdated] = Json.format[EmployeeUpdated]
}

case class EmployeeReleased(id: Long, dor: LocalDate) extends EmployeeEvent

object EmployeeReleased {
  implicit val format: Format[EmployeeReleased] = Json.format[EmployeeReleased]
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

case class PrerogativeIntimationCreated(empId: Long, prerogativeIntimationType: PrerogativeIntimationType, start: LocalDate, end: LocalDate) extends EmployeeEvent

object PrerogativeIntimationCreated {
  implicit val format: Format[PrerogativeIntimationCreated] = Json.format[PrerogativeIntimationCreated]
}

case class PrerogativeIntimationUpdated(empId: Long, prerogativeIntimationType: PrerogativeIntimationType, start: LocalDate, end: LocalDate) extends EmployeeEvent

object PrerogativeIntimationUpdated {
  implicit val format: Format[PrerogativeIntimationUpdated] = Json.format[PrerogativeIntimationUpdated]
}

case class PrerogativeIntimationCancelled(empId: Long, prerogativeIntimationType: PrerogativeIntimationType, start: LocalDate, end: LocalDate) extends EmployeeEvent

object PrerogativeIntimationCancelled {
  implicit val format: Format[PrerogativeIntimationCancelled] = Json.format[PrerogativeIntimationCancelled]
}

case class LastLeavesSaved(empId: Long, earned: Double = 0.0, currentYearEarned: Double = 0.0, sick: Double = 0.0, extra: Double = 0.0) extends EmployeeEvent

object LastLeavesSaved {
  implicit val format: Format[LastLeavesSaved] = Json.format[LastLeavesSaved]
}

case class LeavesCredited(empId: Long, earned: Double, currentYearEarned: Double, sick: Double, extra: Double) extends EmployeeEvent

object LeavesCredited {
  implicit val format: Format[LeavesCredited] = Json.format[LeavesCredited]
}

case class LeavesBalanced(empId: Long, earned: Double, lapsed: Double) extends EmployeeEvent

object LeavesBalanced {
  implicit val format: Format[LeavesBalanced] = Json.format[LeavesBalanced]
}
