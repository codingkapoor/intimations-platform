package com.iamsmkr.holiday.impl.repositories.holiday

import java.time.LocalDate

import slick.jdbc.MySQLProfile.api._

case class HolidayEntity(date: LocalDate, occasion: String)

class HolidayTableDef(tag: Tag) extends Table[HolidayEntity](tag, "holidays") {

  def date = column[LocalDate]("DATE", O.PrimaryKey)

  def occasion = column[String]("OCCASION")

  override def * = (date, occasion).mapTo[HolidayEntity]
}

object HolidayTableDef {
  lazy val holidays = TableQuery[HolidayTableDef]
}
