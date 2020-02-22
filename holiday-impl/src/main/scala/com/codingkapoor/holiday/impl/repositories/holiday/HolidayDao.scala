package com.codingkapoor.holiday.impl.repositories.holiday

import java.time.LocalDate

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class HolidayDao(db: Database) {
  val holidays = HolidayTableDef.holidays

  createTable

  def addHoliday(holiday: HolidayEntity): Future[Int] = {
    db.run(holidays += holiday)
  }

  def getHolidays(start: LocalDate, end: LocalDate): Future[Seq[HolidayEntity]] = {
    db.run(holidays.filter(h => h.date >= start && h.date <= end).result)
  }

  def deleteHoliday(date: LocalDate): Future[Int] = {
    db.run(holidays.filter(h => h.date === date).delete)
  }

  private def createTable: Future[Unit] = db.run(holidays.schema.createIfNotExists)
}
