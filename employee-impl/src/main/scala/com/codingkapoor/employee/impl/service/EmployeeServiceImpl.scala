package com.codingkapoor.employee.impl.service

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, Forbidden, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.slf4j.LoggerFactory
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer.requireAnyRole

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.codingkapoor.employee.api
import com.codingkapoor.employee.api.model._
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.impl.persistence.read.repository.employee.{EmployeeDao, EmployeeEntity}
import com.codingkapoor.employee.impl.persistence.read.repository.intimation.{IntimationDao, IntimationEntity}
import com.codingkapoor.employee.impl.persistence.read.repository.request.{RequestDao, RequestEntity}
import com.codingkapoor.employee.impl.persistence.write._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.lagom.scaladsl.SecuredService

import scala.concurrent.Await

class EmployeeServiceImpl(override val securityConfig: Config, persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeDao,
                          intimationRepository: IntimationDao, requestRepository: RequestDao) extends EmployeeService with SecuredService {

  import EmployeeServiceImpl._

  private val logger = LoggerFactory.getLogger(classOf[EmployeeServiceImpl])

  private def entityRef(id: Long) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id.toString)

  override def addEmployee(): ServiceCall[Employee, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { employee: Employee =>
        validateTokenType(profile)

        val isAdmin = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Admin)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (!isAdmin) {
          logger.error("Admin privileges required")
          throw Forbidden("Authorization failed")
        }

        entityRef(employee.id).ask(AddEmployee(employee)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
        }
      }
    )

  override def updateEmployee(id: Long): ServiceCall[EmployeeInfo, Employee] =
    authorize(requireAnyRole[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { employeeInfo: EmployeeInfo =>
        validateTokenType(profile)

        val isAdmin = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Admin)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (!isAdmin) {
          logger.error("Admin privileges required")
          throw Forbidden("Authorization failed")
        }

        entityRef(id).ask(UpdateEmployee(employeeInfo)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
        }
      }
    )

  override def terminateEmployee(id: Long): ServiceCall[NotUsed, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)

        val isAdmin = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Admin)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (!isAdmin) {
          logger.error("Admin privileges required")
          throw Forbidden("Authorization failed")
        }

        entityRef(id).ask(TerminateEmployee(id)).recover {
          case e: InvalidCommandException => throw BadRequest(e.getMessage)
        }
      }
    )

  // TODO: Admin Only
  // TODO: passwordless service also uses this api. This creates the chicken-egg problem which can only be solved if passwordless service
  //  maintains and updates it's own employee table with the help of kafka events
  //  override def getEmployees(email: Option[String]): ServiceCall[NotUsed, Seq[Employee]] = {
  //    authorize(requireAnyRole[CommonProfile]("Admin"), (profile: CommonProfile) =>
  //      ServerServiceCall { _: NotUsed =>
  //        if (profile.getAttribute("type") == "Refresh")
  //          throw Forbidden("Access token expected")
  //        employeeRepository.getEmployees(email).map(_.map(convertEmployeeReadEntityToEmployee))
  //      }
  //    )
  //  }
  override def getEmployees(email: Option[String]): ServiceCall[NotUsed, Seq[Employee]] = {
    authenticate { _ =>
      ServerServiceCall { _: NotUsed =>
        employeeRepository.getEmployees(email).map(_.map(convertEmployeeReadEntityToEmployee))
      }
    }
  }

  override def getEmployee(id: Long): ServiceCall[NotUsed, Employee] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString, Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)

        val isAdmin = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Admin)
          else {
            logger.error("No employee found to whom the provided access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (profile.getId != id.toString && !isAdmin) {
          logger.error("Employees can access their own data only unless they have admin role")
          throw Forbidden("Authorization failed")
        }

        employeeRepository.getEmployee(id).map { e =>
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
    authorize(requireAnyRole[CommonProfile](Role.Admin.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)

        val isAdmin = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Admin)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (!isAdmin) {
          logger.error("Admin privileges required")
          throw Forbidden("Authorization failed")
        }

        entityRef(id).ask(DeleteEmployee(id)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  override def createIntimation(empId: Long): ServiceCall[IntimationReq, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { intimationReq: IntimationReq =>
        validateTokenType(profile)

        val isEmployee = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Employee)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (profile.getId != empId.toString || !isEmployee) {
          logger.error("Employees can access their own data only provided they have employee privileges")
          throw Forbidden("Authorization failed")
        }

        if (intimationReq.reason.length > 0)
          entityRef(empId).ask(CreateIntimation(empId, intimationReq)).recover {
            case e: InvalidCommandException => throw BadRequest(e.message)
          }
        else
          throw BadRequest("Please provide a valid reason.")
      }
    )

  override def updateIntimation(empId: Long): ServiceCall[IntimationReq, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { intimationReq: IntimationReq =>
        validateTokenType(profile)

        val isEmployee = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Employee)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (profile.getId != empId.toString || !isEmployee) {
          logger.error("Employees can access their own data only provided they have employee privileges")
          throw Forbidden("Authorization failed")
        }

        if (intimationReq.reason.length > 0)
          entityRef(empId).ask(UpdateIntimation(empId, intimationReq)).recover {
            case e: InvalidCommandException => throw BadRequest(e.message)
          }
        else
          throw BadRequest("Please provide a valid reason.")
      }
    )

  override def cancelIntimation(empId: Long): ServiceCall[NotUsed, Done] =
    authorize(requireAnyRole[CommonProfile](Role.Employee.toString), (profile: CommonProfile) =>
      ServerServiceCall { _: NotUsed =>
        validateTokenType(profile)

        val isEmployee = Await.result(employeeRepository.getEmployee(profile.getId.toLong).map { e =>
          if (e.isDefined) e.get.roles.contains(Role.Employee)
          else {
            logger.error("No employee found to whom the access token supposedly belongs to")
            throw Forbidden("Authorization failed")
          }
        }, 5.seconds)

        if (profile.getId != empId.toString || !isEmployee) {
          logger.error("Employees can access their own data only provided they have employee privileges")
          throw Forbidden("Authorization failed")
        }
        entityRef(empId).ask(CancelIntimation(empId)).recover {
          case e: InvalidCommandException => throw BadRequest(e.message)
        }
      }
    )

  // TODO: Employee, Admin
  override def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): ServiceCall[NotUsed, List[InactiveIntimation]] = ServiceCall { _ =>
    intimationRepository.getInactiveIntimations(empId, start, end).map(convertToInactiveIntimations)
  }

  // TODO: Employee, Admin
  override def getActiveIntimations: ServiceCall[NotUsed, List[ActiveIntimation]] = ServiceCall { _ =>
    intimationRepository.getActiveIntimations.map(convertToActiveIntimations)
  }

  override def employeeTopic: Topic[EmployeeKafkaEvent] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(EmployeeEvent.Tag, fromOffset)
        .map(event => (convertPersistentEntityEventToKafkaEvent(event), event.offset))
    }
  }

}

object EmployeeServiceImpl {

  private def validateTokenType(profile: CommonProfile): Unit = {
    if (profile.getAttribute("type") == "Refresh")
      throw Forbidden("Access token expected")
  }

  private def convertPersistentEntityEventToKafkaEvent(eventStreamElement: EventStreamElement[EmployeeEvent]): EmployeeKafkaEvent = {
    eventStreamElement.event match {
      case EmployeeAdded(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles) =>
        EmployeeAddedKafkaEvent(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles)

      case EmployeeUpdated(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles) =>
        EmployeeUpdatedKafkaEvent(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles)

      case EmployeeTerminated(id, _, _, _, _, _, _, _, _, _, _) =>
        EmployeeTerminatedKafkaEvent(id)

      case EmployeeDeleted(id) =>
        EmployeeDeletedKafkaEvent(id)

      case IntimationCreated(empId, reason, lastModified, requests) =>
        IntimationCreatedKafkaEvent(empId, reason, lastModified, requests)

      case IntimationUpdated(empId, reason, lastModified, requests) =>
        IntimationUpdatedKafkaEvent(empId, reason, lastModified, requests)

      case IntimationCancelled(empId, reason, lastModified, requests) =>
        IntimationCancelledKafkaEvent(empId, reason, lastModified, requests)
    }
  }

  private def convertEmployeeReadEntityToEmployee(e: EmployeeEntity): Employee = {
    api.model.Employee(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, ContactInfo(e.phone, e.email),
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
