package com.codingkapoor.notifier.api.model

import java.time.LocalDateTime

import com.codingkapoor.employee.api.model.Request
import com.codingkapoor.notifier.api.model.IntimationType.IntimationType
import play.api.libs.json.{Format, Json}

object IntimationType extends Enumeration {
  type IntimationType = Value
  val Created, Updated, Cancelled = Value

  implicit val format: Format[IntimationType.Value] = Json.formatEnum(this)
}

case class Notification(empId: Long, empName: String, lastModified: LocalDateTime, reason: String, requests: Set[Request], intimationType: IntimationType)

object Notification {
  implicit val format: Format[Notification] = Json.format[Notification]
}
