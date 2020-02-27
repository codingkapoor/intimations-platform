package com.codingkapoor.employee.api.models

import java.time.{LocalDate, LocalDateTime}

import com.codingkapoor.employee.api.models.PrivilegedIntimationType.PrivilegedIntimationType
import com.codingkapoor.employee.api.models.Role.Role
import julienrf.json.derived
import play.api.libs.json._

sealed trait EmployeeKafkaEvent {
  val id: Long
}

object EmployeeKafkaEvent {
  implicit val format: Format[EmployeeKafkaEvent] = derived.flat.oformat((__ \ "type").format[String])
}

case class EmployeeAddedKafkaEvent(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String, pfn: String,
                                   contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeKafkaEvent

object EmployeeAddedKafkaEvent {
  implicit val format: Format[EmployeeAddedKafkaEvent] = Json.format[EmployeeAddedKafkaEvent]
}

case class EmployeeUpdatedKafkaEvent(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String, pfn: String,
                                     contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeKafkaEvent

object EmployeeUpdatedKafkaEvent {
  implicit val format: Format[EmployeeUpdatedKafkaEvent] = Json.format[EmployeeUpdatedKafkaEvent]
}

case class EmployeeReleasedKafkaEvent(id: Long, dor: LocalDate) extends EmployeeKafkaEvent

object EmployeeReleasedKafkaEvent {
  implicit val format: Format[EmployeeReleasedKafkaEvent] = Json.format[EmployeeReleasedKafkaEvent]
}

case class EmployeeDeletedKafkaEvent(id: Long) extends EmployeeKafkaEvent

object EmployeeDeletedKafkaEvent {
  implicit val format: Format[EmployeeDeletedKafkaEvent] = Json.format[EmployeeDeletedKafkaEvent]
}

case class IntimationCreatedKafkaEvent(id: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationCreatedKafkaEvent {
  implicit val format: Format[IntimationCreatedKafkaEvent] = Json.format[IntimationCreatedKafkaEvent]
}

case class IntimationUpdatedKafkaEvent(id: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationUpdatedKafkaEvent {
  implicit val format: Format[IntimationUpdatedKafkaEvent] = Json.format[IntimationUpdatedKafkaEvent]
}

case class IntimationCancelledKafkaEvent(id: Long, reason: String, lastModified: LocalDateTime, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationCancelledKafkaEvent {
  implicit val format: Format[IntimationCancelledKafkaEvent] = Json.format[IntimationCancelledKafkaEvent]
}

case class PrivilegedIntimationCreatedKafkaEvent(id: Long, privilegedIntimationType: PrivilegedIntimationType, start: LocalDate, end: LocalDate) extends EmployeeKafkaEvent

object PrivilegedIntimationCreatedKafkaEvent {
  implicit val format: Format[PrivilegedIntimationCreatedKafkaEvent] = Json.format[PrivilegedIntimationCreatedKafkaEvent]
}

case class PrivilegedIntimationUpdatedKafkaEvent(id: Long, privilegedIntimationType: PrivilegedIntimationType, start: LocalDate, end: LocalDate) extends EmployeeKafkaEvent

object PrivilegedIntimationUpdatedKafkaEvent {
  implicit val format: Format[PrivilegedIntimationUpdatedKafkaEvent] = Json.format[PrivilegedIntimationUpdatedKafkaEvent]
}

case class PrivilegedIntimationCancelledKafkaEvent(id: Long, privilegedIntimationType: PrivilegedIntimationType, start: LocalDate, end: LocalDate) extends EmployeeKafkaEvent

object PrivilegedIntimationCancelledKafkaEvent {
  implicit val format: Format[PrivilegedIntimationCancelledKafkaEvent] = Json.format[PrivilegedIntimationCancelledKafkaEvent]
}

case class LastLeavesSavedKafkaEvent(id: Long, earned: Double, currentYearEarned: Double, sick: Double, extra: Double) extends EmployeeKafkaEvent

object LastLeavesSavedKafkaEvent {
  implicit val format: Format[LastLeavesSavedKafkaEvent] = Json.format[LastLeavesSavedKafkaEvent]
}

case class LeavesCreditedKafkaEvent(id: Long, earned: Double, currentYearEarned: Double, sick: Double, extra: Double) extends EmployeeKafkaEvent

object LeavesCreditedKafkaEvent {
  implicit val format: Format[LeavesCreditedKafkaEvent] = Json.format[LeavesCreditedKafkaEvent]
}

case class LeavesBalancedKafkaEvent(id: Long, earned: Double, lapsed: Double) extends EmployeeKafkaEvent

object LeavesBalancedKafkaEvent {
  implicit val format: Format[LeavesBalancedKafkaEvent] = Json.format[LeavesBalancedKafkaEvent]
}
