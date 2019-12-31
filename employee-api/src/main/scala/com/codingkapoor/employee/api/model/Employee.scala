package com.codingkapoor.employee.api.model

import java.time.{LocalDate, LocalDateTime}
import play.api.libs.json.{Format, Json}

import com.codingkapoor.employee.api.model.RequestType.RequestType
import com.codingkapoor.employee.api.model.Role.Role

case class ContactInfo(phone: String, email: String)

object ContactInfo {
  implicit val format: Format[ContactInfo] = Json.format[ContactInfo]
}

case class Location(city: String = "Bangalore", state: String = "Karnataka", country: String = "India")

object Location {
  implicit val format: Format[Location] = Json.using[Json.WithDefaultValues].format[Location]
}

case class Leaves(earned: Int = 0, sick: Int = 0)

object Leaves {
  implicit val format: Format[Leaves] = Json.using[Json.WithDefaultValues].format[Leaves]
}

object Role extends Enumeration {
  type Role = Value
  val Employee, Admin = Value

  implicit val format: Format[Role.Value] = Json.formatEnum(this)
}

case class Employee(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String,
                    isActive: Boolean = true, contactInfo: ContactInfo, location: Location = Location(), leaves: Leaves = Leaves(), roles: List[Role])

object Employee {
  implicit val format: Format[Employee] = Json.using[Json.WithDefaultValues].format[Employee]
}

// TODO: Employee terminated could always be re-hired
// TODO: Uupdate employee roles
case class EmployeeInfo(designation: Option[String], contactInfo: Option[ContactInfo], location: Option[Location], leaves: Option[Leaves])

object EmployeeInfo {
  implicit val format: Format[EmployeeInfo] = Json.format[EmployeeInfo]
}

object RequestType extends Enumeration {
  type RequestType = Value
  val WFO, WFH, Leave = Value

  implicit val format: Format[RequestType.Value] = Json.formatEnum(this)
}

case class Request(date: LocalDate, firstHalf: RequestType, secondHalf: RequestType)

object Request {
  implicit val format: Format[Request] = Json.format[Request]
}

case class Intimation(reason: String, lastModified: LocalDateTime, requests: Set[Request])

object Intimation {
  implicit val format: Format[Intimation] = Json.format[Intimation]
}

case class IntimationReq(reason: String, requests: Set[Request])

object IntimationReq {
  implicit val format: Format[IntimationReq] = Json.format[IntimationReq]
}

case class InactiveIntimation(id: Long, empId: Long, reason: String, requests: Set[Request])

object InactiveIntimation {
  implicit val format: Format[InactiveIntimation] = Json.format[InactiveIntimation]
}

case class ActiveIntimation(id: Long, empId: Long, empName: String, reason: String, latestRequestDate: LocalDate, lastModified: LocalDateTime, requests: Set[Request])

object ActiveIntimation {
  implicit val format: Format[ActiveIntimation] = Json.format[ActiveIntimation]
}
