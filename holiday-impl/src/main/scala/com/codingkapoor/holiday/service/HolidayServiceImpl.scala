package com.codingkapoor.holiday.service

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.codingkapoor.holiday.api.{Holiday, HolidayService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class HolidayServiceImpl extends HolidayService {

  private val log = LoggerFactory.getLogger(classOf[HolidayServiceImpl])

  override def addHoliday(): ServiceCall[Holiday, Done] = ServiceCall { _ =>
    Future.successful(Done)
  }

  override def deleteHoliday(id: Long): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    Future.successful(Done)
  }

  override def getHolidays: ServiceCall[NotUsed, Seq[Holiday]] = ServiceCall { _ =>
    Future.successful(Seq(Holiday(LocalDate.now(), "Happy Birthday")))
  }
}
