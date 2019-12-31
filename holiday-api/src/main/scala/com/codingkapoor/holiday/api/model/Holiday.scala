package com.codingkapoor.holiday.api.model

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class Holiday(date: LocalDate, occasion: String)

object Holiday {
  implicit val format: Format[Holiday] = Json.format[Holiday]
}
