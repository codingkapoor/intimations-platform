package com.codingkapoor.employee.api.model

import java.time.{LocalDate, LocalDateTime}

import com.codingkapoor.employee.api.model.Role.Role
import julienrf.json.derived
import play.api.libs.json._

sealed trait EmployeeKafkaEvent {
  val id: Long
}

object EmployeeKafkaEvent {
  implicit val format: Format[EmployeeKafkaEvent] = derived.flat.oformat((__ \ "type").format[String])
}

case class EmployeeAddedKafkaEvent(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String,
                                   isActive: Boolean, contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeKafkaEvent

object EmployeeAddedKafkaEvent {
  implicit val format: Format[EmployeeAddedKafkaEvent] = Json.format[EmployeeAddedKafkaEvent]
}

case class EmployeeUpdatedKafkaEvent(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String,
                                     isActive: Boolean, contactInfo: ContactInfo, location: Location, leaves: Leaves, roles: List[Role]) extends EmployeeKafkaEvent

object EmployeeUpdatedKafkaEvent {
  implicit val format: Format[EmployeeUpdatedKafkaEvent] = Json.format[EmployeeUpdatedKafkaEvent]
}

case class EmployeeTerminatedKafkaEvent(id: Long) extends EmployeeKafkaEvent

object EmployeeTerminatedKafkaEvent {
  implicit val format: Format[EmployeeTerminatedKafkaEvent] = Json.format[EmployeeTerminatedKafkaEvent]
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
