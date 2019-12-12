package com.codingkapoor.holiday.repository

import slick.jdbc.MySQLProfile.api._

case class HolidayEntity(date: Int, month: Int, year: Int, occasion: String)

class HolidayTableDef(tag: Tag) extends Table[HolidayEntity](tag, "holiday") {

  def date = column[Int]("DATE")

  def month = column[Int]("MONTH")

  def year = column[Int]("YEAR")

  def occasion = column[String]("OCCASION")

  override def * = (date, month, year, occasion).mapTo[HolidayEntity]

  def pk = primaryKey("pk_holiday", (date, month, year))
}

object HolidayTableDef {
  lazy val holidays = TableQuery[HolidayTableDef]
}
