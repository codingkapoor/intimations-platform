package com.codingkapoor.holiday.impl.service

import java.time.LocalDate

import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.codingkapoor.holiday.api.HolidayService
import com.codingkapoor.holiday.api.model.Holiday
import com.codingkapoor.holiday.impl.repository.{HolidayDao, HolidayEntity}
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import org.pac4j.core.config.Config
import org.pac4j.lagom.scaladsl.SecuredService

class HolidayServiceImpl(override val securityConfig: Config, holidayDao: HolidayDao) extends HolidayService with SecuredService {

  import HolidayServiceImpl._

  private val logger = LoggerFactory.getLogger(classOf[HolidayServiceImpl])

  override def addHoliday(): ServiceCall[Holiday, Done] = ServiceCall { holiday =>
    holidayDao.addHoliday(HolidayEntity(holiday.date, holiday.occasion)).map(_ => Done)
  }

  override def getHolidays(start: LocalDate, end: LocalDate): ServiceCall[NotUsed, Seq[Holiday]] = ServiceCall { _ =>
    if (start.isAfter(end)) throw BadRequest("Start date must come before end date.")
    holidayDao.getHolidays(start, end).map(_.map(convertHolidayEntityToHoliday))
  }

  override def deleteHoliday(date: LocalDate): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    holidayDao.deleteHoliday(date).map(_ => Done)
  }

}

object HolidayServiceImpl {
  def convertHolidayEntityToHoliday(e: HolidayEntity): Holiday = {
    Holiday(e.date, e.occasion)
  }
}
