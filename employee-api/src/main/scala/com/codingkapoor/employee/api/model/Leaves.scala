package com.codingkapoor.employee.api.model

import play.api.libs.json.{Format, Json}

case class Leaves(earned: Int = 0, sick: Int = 0)

object Leaves {
  implicit val format: Format[Leaves] = Json.using[Json.WithDefaultValues].format[Leaves]
}
