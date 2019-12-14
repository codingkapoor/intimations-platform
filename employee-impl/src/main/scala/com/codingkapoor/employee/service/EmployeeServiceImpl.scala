package com.codingkapoor.employee.service

import java.time.LocalDate
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global

import com.codingkapoor.employee.api
import com.codingkapoor.employee.api.model._
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.persistence.read.repository.employee.{EmployeeEntity, EmployeeDao}
import com.codingkapoor.employee.persistence.read.repository.intimation.{IntimationEntity, IntimationDao}
import com.codingkapoor.employee.persistence.read.repository.request.{RequestEntity, RequestDao}
import com.codingkapoor.employee.persistence.write._

class EmployeeServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeDao,
                          intimationRepository: IntimationDao, requestRepository: RequestDao) extends EmployeeService {

  import EmployeeServiceImpl._

  private val log = LoggerFactory.getLogger(classOf[EmployeeServiceImpl])

  private def entityRef(id: Long) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id.toString)

  override def addEmployee(): ServiceCall[Employee, Done] = ServiceCall { employee =>
    entityRef(employee.id).ask(AddEmployee(employee)).recover {
      case e: InvalidCommandException => throw BadRequest(e.getMessage)
    }
  }

  override def updateEmployee(id: Long): ServiceCall[EmployeeInfo, Employee] = ServiceCall { employeeInfo =>
    entityRef(id).ask(UpdateEmployee(employeeInfo)).recover {
      case e: InvalidCommandException => throw BadRequest(e.getMessage)
    }
  }

  override def terminateEmployee(id: Long): ServiceCall[NotUsed, Done] = { _ =>
    entityRef(id).ask(TerminateEmployee(id)).recover {
      case e: InvalidCommandException => throw BadRequest(e.getMessage)
    }
  }

  override def getEmployees: ServiceCall[NotUsed, Seq[Employee]] = ServiceCall { _ =>
    employeeRepository.getEmployees.map(_.map(convertEmployeeReadEntityToEmployee))
  }

  override def getEmployee(id: Long): ServiceCall[NotUsed, Employee] = ServiceCall { _ =>
    employeeRepository.getEmployee(id).map { e =>
      if (e.isDefined) convertEmployeeReadEntityToEmployee(e.get)
      else {
        val msg = s"No employee found with id = $id."
        log.error(msg)

        throw NotFound(msg)
      }
    }
  }

  override def deleteEmployee(id: Long): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    entityRef(id).ask(DeleteEmployee(id)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def createIntimation(empId: Long): ServiceCall[IntimationReq, Done] = ServiceCall { intimationReq =>
    entityRef(empId).ask(CreateIntimation(empId, intimationReq)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def updateIntimation(empId: Long): ServiceCall[IntimationReq, Done] = ServiceCall { intimationReq =>
    entityRef(empId).ask(UpdateIntimation(empId, intimationReq)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def cancelIntimation(empId: Long): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    entityRef(empId).ask(CancelIntimation(empId)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): ServiceCall[NotUsed, List[InactiveIntimation]] = ServiceCall { _ =>
    intimationRepository.getInactiveIntimations(empId, start, end).map(convertToInactiveIntimations)
  }

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

  private def convertPersistentEntityEventToKafkaEvent(eventStreamElement: EventStreamElement[EmployeeEvent]): EmployeeKafkaEvent = {
    eventStreamElement.event match {
      case EmployeeAdded(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves) =>
        EmployeeAddedKafkaEvent(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves)

      case EmployeeUpdated(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves) =>
        EmployeeUpdatedKafkaEvent(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves)

      case EmployeeTerminated(id, _, _, _, _, _, _, _, _, _) =>
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
      Location(e.city, e.state, e.country), Leaves(e.earnedLeaves, e.sickLeaves))
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
                ie.id -> (ie.reason, ie.lastModified, requests)
            }
            .map { case (id, t) => ActiveIntimation(id, ee.id, ee.name, t._1, t._2, t._3) }
      }
      .toList
  }

}
