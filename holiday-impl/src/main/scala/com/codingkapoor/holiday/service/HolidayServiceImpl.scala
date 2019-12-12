package com.codingkapoor.holiday.service

import java.time.LocalDate
import akka.{Done, NotUsed}
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.codingkapoor.holiday.api.{Holiday, HolidayService, MonthYear}
import com.codingkapoor.holiday.repository.{HolidayDao, HolidayEntity}

class HolidayServiceImpl(holidayDao: HolidayDao) extends HolidayService {

  import HolidayServiceImpl._

  private val log = LoggerFactory.getLogger(classOf[HolidayServiceImpl])

  override def addHoliday(): ServiceCall[Holiday, Done] = ServiceCall { holiday =>
    holidayDao.addHoliday(HolidayEntity(holiday.date.getDayOfMonth, holiday.date.getMonthValue, holiday.date.getYear, holiday.occasion)).map(_ => Done)
  }

  override def deleteHoliday(date: LocalDate): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    holidayDao.deleteHoliday(date).map(_ => Done)
  }

  override def getHolidays(start: MonthYear, end: MonthYear): ServiceCall[NotUsed, Seq[Holiday]] = ServiceCall { _ =>
    holidayDao.getHolidays(start, end).map(_.map(convertHolidayEntityToHoliday))
  }
}

object HolidayServiceImpl {
  def convertHolidayEntityToHoliday(e: HolidayEntity): Holiday = {
    Holiday(LocalDate.parse(s"""${e.year}-${"%02d".format(e.month)}-${"%02d".format(e.date)}"""), e.occasion)
  }
}
