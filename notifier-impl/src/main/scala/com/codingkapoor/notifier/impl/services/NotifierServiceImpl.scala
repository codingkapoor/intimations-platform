package com.codingkapoor.notifier.impl.services

import akka.{Done, NotUsed}
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.models.Role
import com.codingkapoor.notifier.api.NotifierService
import com.codingkapoor.notifier.impl.repositories.employee.EmployeeDao
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, Forbidden}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer.requireAnyRole
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.scaladsl.SecuredService
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global

class NotifierServiceImpl(override val securityConfig: Config, override val employeeService: EmployeeService,
                          override val employeeDao: EmployeeDao, override val mailNotifier: MailNotifier,
                          override val pushNotifier: PushNotifier) extends NotifierService with EmployeeKafkaEventHandler with SecuredService {

  import NotifierServiceImpl._

  override val logger: Logger = LoggerFactory.getLogger(classOf[NotifierServiceImpl])

  override def subscribe(empId: Long): ServiceCall[ExpoToken, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { expoToken: String =>
        validateTokenType(profile)

        employeeDao.getEmployee(empId).flatMap { employeeOpt =>
          if (employeeOpt.isDefined) {
            employeeDao.updateEmployee(employeeOpt.get.copy(expoToken = Some(expoToken))).map { _ =>
              Done.done()
            }
          } else throw BadRequest(s"No employee found with id = $empId")
        }
      }
    )

  override def unsubscribe(empId: Long): ServiceCall[NotUsed, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        employeeDao.getEmployee(empId).flatMap { employeeOpt =>
          if (employeeOpt.isDefined) {
            employeeDao.updateEmployee(employeeOpt.get.copy(expoToken = None)).map { _ =>
              Done.done()
            }
          } else throw BadRequest(s"No employee found with id = $empId")
        }
      }
    )
}

object NotifierServiceImpl {
  def validateTokenType(profile: CommonProfile): Unit = {
    if (profile.getAttribute("type") == "Refresh")
      throw Forbidden("Access token expected")
  }
}
