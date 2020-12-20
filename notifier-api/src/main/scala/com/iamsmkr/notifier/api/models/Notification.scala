package com.iamsmkr.notifier.api.models

import java.time.LocalDateTime

import com.iamsmkr.employee.api.models.Request
import com.iamsmkr.notifier.api.models.IntimationType.IntimationType
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
