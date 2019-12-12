package com.codingkapoor.holiday.repository

import java.time.LocalDate

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import com.codingkapoor.holiday.api.MonthYear

class HolidayDao(db: Database) {
  val holidays = HolidayTableDef.holidays

  def addHoliday(holiday: HolidayEntity): Future[Int] = {
    db.run(holidays += holiday)
  }

  def getHolidays(start: MonthYear, end: MonthYear): Future[Seq[HolidayEntity]] = {
    db.run(holidays.result)
  }

  def deleteHoliday(date: LocalDate): Future[Int] = {
    db.run(holidays.filter(i => i.date === date.getDayOfMonth && i.month === date.getMonthValue && i.year === date.getYear).delete)
  }
}
