package com.codingkapoor.holiday.service

import akka.{Done, NotUsed}
import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.codingkapoor.holiday.api.{Holiday, HolidayRes, HolidayService, MonthYear}
import com.codingkapoor.holiday.repository.{HolidayDao, HolidayEntity}

class HolidayServiceImpl(holidayDao: HolidayDao) extends HolidayService {

  import HolidayServiceImpl._

  private val log = LoggerFactory.getLogger(classOf[HolidayServiceImpl])

  override def addHoliday(): ServiceCall[Holiday, Done] = ServiceCall { holiday =>
    holidayDao.addHoliday(HolidayEntity(holiday.date, holiday.occasion)).map(_ => Done)
  }

  override def deleteHoliday(id: Long): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    holidayDao.deleteHoliday(id).map(_ => Done)
  }

  override def getHolidays(start: MonthYear, end: MonthYear): ServiceCall[NotUsed, Seq[HolidayRes]] = ServiceCall { _ =>
    holidayDao.getHolidays(start, end).map(_.map(convertHolidayEntityToHolidayRes))
  }
}

object HolidayServiceImpl {
  def convertHolidayEntityToHolidayRes(he: HolidayEntity): HolidayRes = {
    HolidayRes(he.id, he.date, he.occasion)
  }
}
