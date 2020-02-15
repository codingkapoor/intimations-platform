package com.codingkapoor.notifier.impl.service

import akka.{Done, NotUsed}
import com.codingkapoor.notifier.api.NotifierService
import com.codingkapoor.notifier.impl.repository.employee.EmployeeDao
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global

class NotifierServiceImpl(employeeDao: EmployeeDao) extends NotifierService {

  val logger: Logger = LoggerFactory.getLogger(classOf[NotifierServiceImpl])

  override def subscribe(empId: Long): ServiceCall[ExpoToken, Done] = ServiceCall { expoToken =>
    employeeDao.getEmployee(empId).flatMap { employeeOpt =>
      if (employeeOpt.isDefined) {
        employeeDao.updateEmployee(employeeOpt.get.copy(expoToken = Some(expoToken))).map { _ =>
          Done
        }
      } else throw BadRequest(s"No employee found with id = $empId")
    }
  }

  override def unsubscribe(empId: Long): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    employeeDao.getEmployee(empId).flatMap { employeeOpt =>
      if (employeeOpt.isDefined) {
        employeeDao.updateEmployee(employeeOpt.get.copy(expoToken = None)).map { _ =>
          Done
        }
      } else throw BadRequest(s"No employee found with id = $empId")
    }
  }
}
