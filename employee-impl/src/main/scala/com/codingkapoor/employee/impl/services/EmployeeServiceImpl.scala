package com.codingkapoor.employee.impl.services

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.slf4j.{Logger, LoggerFactory}
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer.requireAnyRole
import org.pac4j.core.authorization.authorizer.RequireAllRolesAuthorizer.requireAllRoles

import scala.concurrent.ExecutionContext.Implicits.global
import com.codingkapoor.employee.api
import com.codingkapoor.employee.api.models._
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.impl.persistence.read.repositories.employee.{EmployeeDao, EmployeeEntity}
import com.codingkapoor.employee.impl.persistence.read.repositories.intimation.{IntimationDao, IntimationEntity}
import com.codingkapoor.employee.impl.persistence.read.repositories.request.{RequestDao, RequestEntity}
import com.codingkapoor.employee.impl.persistence.write._
import com.codingkapoor.employee.impl.persistence.write.models._
import com.codingkapoor.employee.impl.utils.AuthValidator
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.scaladsl.SecuredService

class EmployeeServiceImpl(override val securityConfig: Config, persistentEntityRegistry: PersistentEntityRegistry, override val employeeDao: EmployeeDao,
                          intimationDao: IntimationDao, requestDao: RequestDao) extends EmployeeService with SecuredService with AuthValidator {

  import EmployeeServiceImpl._

  override val logger: Logger = LoggerFactory.getLogger(classOf[EmployeeServiceImpl])

  private def entityRef(id: Long) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id.toString)

  override def addEmployee(): ServiceCall[Employee, Done] =
    authorize(requireAllRoles[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { employee: Employee =>
        validateTokenType(profile)
        validateIfProfileBelongsToAdmin(profile)

        entityRef(employee.id).ask(AddEmployee(employee)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
            // TODO: How about handling general exception and logging the same
        }
      }
    )

  override def updateEmployee(id: Long): ServiceCall[EmployeeInfo, Employee] =
    authorize(requireAllRoles[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { employeeInfo: EmployeeInfo =>
        validateTokenType(profile)
        validateIfProfileBelongsToAdmin(profile)

        if (profile.getId == id.toString && employeeInfo.roles.isDefined && !employeeInfo.roles.get.contains(Role.Admin))
          throw BadRequest("Admins can't revoke their own admin privileges")
        else entityRef(id).ask(UpdateEmployee(id, employeeInfo)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
        }
      }
    )

  override def releaseEmployee(id: Long): ServiceCall[NotUsed, Done] =
    authorize(requireAllRoles[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToAdmin(profile)

        entityRef(id).ask(ReleaseEmployee(id)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
        }
      }
    )

  override def getEmployees(email: Option[String]): ServiceCall[NotUsed, Seq[Employee]] =
    authorize(requireAllRoles[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToAdmin(profile)

        employeeDao.getEmployees(email).map(_.map(convertEmployeeReadEntityToEmployee))
      }
    )

  override def getEmployee(id: Long): ServiceCall[NotUsed, Employee] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToIndividualEmployeeOrAdmin(profile, id)

        employeeDao.getEmployee(id).map { e =>
          if (e.isDefined) convertEmployeeReadEntityToEmployee(e.get)
          else {
            val msg = s"No employee found with id = $id."
            logger.error(msg)

            throw NotFound(msg)
          }
        }
      }
    )

  override def deleteEmployee(id: Long): ServiceCall[NotUsed, Done] =
    authorize(requireAllRoles[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToAdmin(profile)

        entityRef(id).ask(DeleteEmployee(id)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  override def createIntimation(empId: Long): ServiceCall[IntimationReq, Leaves] =
    authorize(requireAllRoles[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { intimationReq: IntimationReq =>
        validateTokenType(profile)
        validateIfProfileBelongsToIndividualEmployee(profile, empId)

        entityRef(empId).ask(CreateIntimation(empId, intimationReq)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  override def updateIntimation(empId: Long): ServiceCall[IntimationReq, Leaves] =
    authorize(requireAllRoles[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { intimationReq: IntimationReq =>
        validateTokenType(profile)
        validateIfProfileBelongsToIndividualEmployee(profile, empId)

        entityRef(empId).ask(UpdateIntimation(empId, intimationReq)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  override def cancelIntimation(empId: Long): ServiceCall[NotUsed, Leaves] =
    authorize(requireAllRoles[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToIndividualEmployee(profile, empId)

        entityRef(empId).ask(CancelIntimation(empId)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  override def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): ServiceCall[NotUsed, List[InactiveIntimation]] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToIndividualEmployeeOrAdmin(profile, empId)

        intimationDao.getInactiveIntimations(empId, start, end).map(convertToInactiveIntimations)
      }
    )

  override def getActiveIntimations: ServiceCall[NotUsed, List[ActiveIntimation]] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)
        validateIfProfileBelongsToAnyEmployeeOrAdmin(profile)

        intimationDao.getActiveIntimations.map(convertToActiveIntimations)
      }
    )

  override def createPrivilegedIntimation(empId: Long): ServiceCall[PrivilegedIntimation, Leaves] = ServiceCall[PrivilegedIntimation, Leaves] { privilegedIntimation =>
    entityRef(empId).ask(CreatePrivilegedIntimation(empId, privilegedIntimation)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def updatePrivilegedIntimation(empId: Long): ServiceCall[PrivilegedIntimation, Leaves] = ServiceCall[PrivilegedIntimation, Leaves] { privilegedIntimation =>
    entityRef(empId).ask(UpdatePrivilegedIntimation(empId, privilegedIntimation)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def cancelPrivilegedIntimation(empId: Long): ServiceCall[NotUsed, Leaves] = ServiceCall[NotUsed, Leaves] { _ =>
    entityRef(empId).ask(CancelPrivilegedIntimation(empId)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def employeeTopic: Topic[EmployeeKafkaEvent] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(EmployeeEvent.Tag, fromOffset)
        .map(event => (convertPersistentEntityEventToKafkaEvent(event), event.offset))
    }
  }
}

object EmployeeServiceImpl {

  private def convertPersistentEntityEventToKafkaEvent(eventStreamElement: EventStreamElement[EmployeeEvent]): EmployeeKafkaEvent = {
    eventStreamElement.event match {
      case EmployeeAdded(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles) =>
        EmployeeAddedKafkaEvent(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles)

      case EmployeeUpdated(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles) =>
        EmployeeUpdatedKafkaEvent(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles)

      case EmployeeReleased(id, dor) =>
        EmployeeReleasedKafkaEvent(id, dor)

      case EmployeeDeleted(id) =>
        EmployeeDeletedKafkaEvent(id)

      case IntimationCreated(empId, reason, lastModified, requests) =>
        IntimationCreatedKafkaEvent(empId, reason, lastModified, requests)

      case IntimationUpdated(empId, reason, lastModified, requests) =>
        IntimationUpdatedKafkaEvent(empId, reason, lastModified, requests)

      case IntimationCancelled(empId, reason, lastModified, requests) =>
        IntimationCancelledKafkaEvent(empId, reason, lastModified, requests)

      case LastLeavesSaved(empId, earned, currentYearEarned, sick, extra) =>
        LastLeavesSavedKafkaEvent(empId, earned, currentYearEarned, sick, extra)

      case LeavesCredited(empId, earned, currentYearEarned, sick, extra) =>
        LeavesCreditedKafkaEvent(empId, earned, currentYearEarned, sick, extra)

      case LeavesBalanced(empId, earned, lapsed) =>
        LeavesBalancedKafkaEvent(empId, earned, lapsed)
    }
  }

  private def convertEmployeeReadEntityToEmployee(e: EmployeeEntity): Employee = {
    api.models.Employee(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, ContactInfo(e.phone, e.email),
      Location(e.city, e.state, e.country), Leaves(e.earnedLeaves, e.sickLeaves), e.roles)
  }

  private def convertToInactiveIntimations(s: Seq[(IntimationEntity, RequestEntity)]): List[InactiveIntimation] = {
    s.groupBy { case (ie, _) => ie.empId } // group by employees
      .flatMap {
        case (empId, s) =>
          s.groupBy { case (ie, _) => ie } // group by intimations per employee so as to prepare requests per intimation
            .map {
              case (ie, s) =>
                val requests = s.map { case (_, re) => Request(re.date, re.firstHalf, re.secondHalf) }.toSet
                ie.id -> (ie.reason, requests)
            }
            .map { case (id, t) => InactiveIntimation(id, empId, t._1, t._2) }
      }
      .toList
  }

  private def convertToActiveIntimations(s: Seq[((EmployeeEntity, IntimationEntity), RequestEntity)]): List[ActiveIntimation] = {
    s.groupBy { case ((ee, _), _) => ee } // group by employees
      .flatMap {
        case (ee, s) =>
          s.groupBy { case ((_, ie), _) => ie } // group by intimations per employee so as to prepare requests per intimation
            .map {
              case (ie, s) =>
                val requests = s.map { case ((_, _), re) => Request(re.date, re.firstHalf, re.secondHalf) }.toSet
                ie.id -> (ie.reason, ie.latestRequestDate, ie.lastModified, requests)
            }
            .map { case (id, t) => ActiveIntimation(id, ee.id, ee.name, t._1, t._2, t._3, t._4) }
      }
      .toList
  }

}
