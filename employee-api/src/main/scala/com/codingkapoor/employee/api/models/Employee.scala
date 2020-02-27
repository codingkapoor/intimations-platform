package com.codingkapoor.employee.api.models

import java.time.{LocalDate, LocalDateTime}

import com.codingkapoor.employee.api.models.PrivilegedIntimationType.PrivilegedIntimationType
import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.models.RequestType.RequestType
import com.codingkapoor.employee.api.models.Role.Role

case class ContactInfo(phone: String, email: String)

object ContactInfo {
  implicit val format: Format[ContactInfo] = Json.format[ContactInfo]
}

case class Location(city: String = "Bangalore", state: String = "Karnataka", country: String = "India")

object Location {
  implicit val format: Format[Location] = Json.using[Json.WithDefaultValues].format[Location]
}

case class Leaves(earned: Double = 0.0, currentYearEarned: Double = 0.0, sick: Double = 0.0, extra: Double = 0.0)

object Leaves {
  implicit val format: Format[Leaves] = Json.using[Json.WithDefaultValues].format[Leaves]
}

object Role extends Enumeration {
  type Role = Value
  val Employee, Admin = Value

  implicit val format: Format[Role.Value] = Json.formatEnum(this)
}

case class Employee(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate] = None, designation: String, pfn: String,
                    contactInfo: ContactInfo, location: Location = Location(), leaves: Leaves = Leaves(), roles: List[Role])

object Employee {
  implicit val format: Format[Employee] = Json.using[Json.WithDefaultValues].format[Employee]
}

case class EmployeeInfo(designation: Option[String], contactInfo: Option[ContactInfo], location: Option[Location], roles: Option[List[Role]])

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

object PrivilegedIntimationType extends Enumeration {
  type PrivilegedIntimationType = Value
  val Maternity, Paternity, Sabbatical = Value

  implicit val format: Format[PrivilegedIntimationType.Value] = Json.formatEnum(this)
}

case class PrivilegedIntimation(privilegedIntimationType: PrivilegedIntimationType, start: LocalDate, end: LocalDate)

object PrivilegedIntimation {
  implicit val format: Format[PrivilegedIntimation] = Json.format[PrivilegedIntimation]
}

case class InactiveIntimation(id: Long, empId: Long, reason: String, requests: Set[Request])

object InactiveIntimation {
  implicit val format: Format[InactiveIntimation] = Json.format[InactiveIntimation]
}

case class ActiveIntimation(id: Long, empId: Long, empName: String, reason: String, latestRequestDate: LocalDate, lastModified: LocalDateTime, requests: Set[Request])

object ActiveIntimation {
  implicit val format: Format[ActiveIntimation] = Json.format[ActiveIntimation]
}
