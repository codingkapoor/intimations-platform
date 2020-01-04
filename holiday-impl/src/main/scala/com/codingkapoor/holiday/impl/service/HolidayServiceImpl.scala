package com.codingkapoor.holiday.impl.service

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.codingkapoor.employee.api.model.Role

import scala.concurrent.ExecutionContext.Implicits.global
import org.slf4j.LoggerFactory
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.codingkapoor.holiday.api.HolidayService
import com.codingkapoor.holiday.api.model.Holiday
import com.codingkapoor.holiday.impl.repository.{HolidayDao, HolidayEntity}
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, Forbidden}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer.requireAnyRole
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.scaladsl.SecuredService

class HolidayServiceImpl(override val securityConfig: Config, holidayDao: HolidayDao) extends HolidayService with SecuredService {

  import HolidayServiceImpl._

  private val logger = LoggerFactory.getLogger(classOf[HolidayServiceImpl])

  override def addHoliday(): ServiceCall[Holiday, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { holiday =>
        validateTokenType(profile)

        holidayDao.addHoliday(HolidayEntity(holiday.date, holiday.occasion)).map(_ => Done)
      }
    )

  override def getHolidays(start: LocalDate, end: LocalDate): ServiceCall[NotUsed, Seq[Holiday]] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _ =>
        validateTokenType(profile)

        if (start.isAfter(end)) {
          logger.error(s"Start date $start must come before end date $end")
          throw BadRequest("Start date must come before end date")
        }
        holidayDao.getHolidays(start, end).map(_.map(convertHolidayEntityToHoliday))
      }
    )

  override def deleteHoliday(date: LocalDate): ServiceCall[NotUsed, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _ =>
        validateTokenType(profile)

        holidayDao.deleteHoliday(date).map(_ => Done)
      }
    )
}

object HolidayServiceImpl {
  def validateTokenType(profile: CommonProfile): Unit = {
    if (profile.getAttribute("type") == "Refresh")
      throw Forbidden("Access token expected")
  }

  def convertHolidayEntityToHoliday(e: HolidayEntity): Holiday = {
    Holiday(e.date, e.occasion)
  }
}
