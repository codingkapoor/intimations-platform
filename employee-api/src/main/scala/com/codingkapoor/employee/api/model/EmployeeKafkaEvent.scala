package com.codingkapoor.employee.api.model

import java.time.LocalDate
import julienrf.json.derived
import play.api.libs.json._

sealed trait EmployeeKafkaEvent {
  val id: String
}

object EmployeeKafkaEvent {
  implicit val format: Format[EmployeeKafkaEvent] = derived.flat.oformat((__ \ "type").format[String])
}

case class EmployeeAddedKafkaEvent(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, leaves: Leaves) extends EmployeeKafkaEvent

object EmployeeAddedKafkaEvent {
  implicit val format: Format[EmployeeAddedKafkaEvent] = Json.format[EmployeeAddedKafkaEvent]
}

case class EmployeeTerminatedKafkaEvent(id: String) extends EmployeeKafkaEvent

object EmployeeTerminatedKafkaEvent {
  implicit val format: Format[EmployeeTerminatedKafkaEvent] = Json.format[EmployeeTerminatedKafkaEvent]
}

case class EmployeeDeletedKafkaEvent(id: String) extends EmployeeKafkaEvent

object EmployeeDeletedKafkaEvent {
  implicit val format: Format[EmployeeDeletedKafkaEvent] = Json.format[EmployeeDeletedKafkaEvent]
}

case class IntimationCreatedKafkaEvent(id: String, reason: String, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationCreatedKafkaEvent {
  implicit val format: Format[IntimationCreatedKafkaEvent] = Json.format[IntimationCreatedKafkaEvent]
}

case class IntimationUpdatedKafkaEvent(id: String, reason: String, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationUpdatedKafkaEvent {
  implicit val format: Format[IntimationUpdatedKafkaEvent] = Json.format[IntimationUpdatedKafkaEvent]
}

case class IntimationCancelledKafkaEvent(id: String, reason: String, requests: Set[Request]) extends EmployeeKafkaEvent

object IntimationCancelledKafkaEvent {
  implicit val format: Format[IntimationCancelledKafkaEvent] = Json.format[IntimationCancelledKafkaEvent]
}
