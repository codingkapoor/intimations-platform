package com.codingkapoor.employees.api

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class Employee(id: String, name: String, gender: String, doj: LocalDate, pfn: String) {
  require(
    id != null && id.trim.nonEmpty
      && name != null && name.trim.nonEmpty
      && gender != null && gender.trim.nonEmpty
      && doj != null
      && pfn != null && pfn.trim.nonEmpty,
    "Id, Name, Genderm Date of joining & PF No. are mandatory information."
  )
}

object Employee {
  implicit val format: Format[Employee] = Json.format[Employee]
}
