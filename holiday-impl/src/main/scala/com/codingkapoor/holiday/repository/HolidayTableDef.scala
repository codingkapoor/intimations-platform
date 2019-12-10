package com.codingkapoor.holiday.repository

import java.time.LocalDate
import slick.jdbc.MySQLProfile.api._

case class HolidayEntity(date: LocalDate, occasion: String, id: Long = 0L)

class HolidayTableDef(tag: Tag) extends Table[HolidayEntity](tag, "holiday") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def date = column[LocalDate]("DATE")

  def occasion = column[String]("OCCASION")

  override def * = (date, occasion, id).mapTo[HolidayEntity]
}

object HolidayTableDef {
  lazy val holidays = TableQuery[HolidayTableDef]
}
