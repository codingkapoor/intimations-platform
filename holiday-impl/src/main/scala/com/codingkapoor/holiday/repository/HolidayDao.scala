package com.codingkapoor.holiday.repository

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future

import com.codingkapoor.holiday.api.MonthYear

class HolidayDao(db: Database) {
  val holidays = HolidayTableDef.holidays

  def addHoliday(holiday: HolidayEntity): Future[Long] = {
    db.run((holidays returning holidays.map(_.id)) += holiday)
  }

  def getHolidays(start: MonthYear, end: MonthYear): Future[Seq[HolidayEntity]] = {
    db.run(holidays.result)
  }

  def deleteHoliday(id: Long): Future[Int] = {
    db.run(holidays.filter(i => i.id === id).delete)
  }
}
