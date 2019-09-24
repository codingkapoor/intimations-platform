package com.codingkapoor.employee.api.model

import java.time.LocalDate
import play.api.libs.json.{Format, Json}

import com.codingkapoor.employee.api.model
import com.codingkapoor.employee.api.model.RequestType.RequestType

case class Leaves(earned: Int = 0, sick: Int = 0)

object Leaves {
  implicit val format: Format[Leaves] = Json.using[Json.WithDefaultValues].format[Leaves]
}

case class Employee(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean = true, leaves: Leaves = Leaves())

object Employee {
  implicit val format: Format[Employee] = Json.using[Json.WithDefaultValues].format[Employee]
}

object RequestType extends Enumeration {
  type RequestType = Value
  val FirstHalfWfh, SecondHalfWfh, FullDayWfh, FirstHalfLeave, SecondHalfLeave, FullDayLeave = Value

  implicit val format: Format[model.RequestType.Value] = Json.formatEnum(this)
}

case class Request(date: LocalDate, requestType: RequestType)

object Request {
  implicit val format: Format[Request] = Json.format[Request]
}

case class IntimationReq(reason: String, requests: Set[Request])

object IntimationReq {
  implicit val format: Format[IntimationReq] = Json.format[IntimationReq]
}

case class IntimationRes(empId: String, reason: String, requests: Set[Request])

object IntimationRes {
  implicit val format: Format[IntimationRes] = Json.format[IntimationRes]
}
