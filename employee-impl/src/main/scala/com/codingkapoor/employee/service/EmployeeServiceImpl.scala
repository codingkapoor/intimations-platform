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
import com.codingkapoor.employee.api.model.{Employee, EmployeeAddedKafkaEvent, EmployeeDeletedKafkaEvent, EmployeeKafkaEvent, EmployeeTerminatedKafkaEvent, IntimationCancelledKafkaEvent, IntimationCreatedKafkaEvent, IntimationReq, IntimationRes, IntimationUpdatedKafkaEvent, Leaves, Request}
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.persistence.read.dao.employee.{EmployeeEntity, EmployeeRepository}
import com.codingkapoor.employee.persistence.read.dao.intimation.{IntimationEntity, IntimationRepository}
import com.codingkapoor.employee.persistence.read.dao.request.{RequestEntity, RequestRepository}
import com.codingkapoor.employee.persistence.write._

class EmployeeServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeRepository,
                          intimationRepository: IntimationRepository, requestRepository: RequestRepository) extends EmployeeService {

  import EmployeeServiceImpl._

  private val log = LoggerFactory.getLogger(classOf[EmployeeServiceImpl])

  private def entityRef(id: String) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id)

  override def addEmployee(): ServiceCall[Employee, Done] = ServiceCall { employee =>
    entityRef(employee.id).ask(AddEmployee(employee)).recover {
      case e: InvalidCommandException => throw BadRequest(e.getMessage)
    }
  }

  override def terminateEmployee(id: String): ServiceCall[NotUsed, Done] = { _ =>
    entityRef(id).ask(TerminateEmployee(id)).recover {
      case e: InvalidCommandException => throw BadRequest(e.getMessage)
    }
  }

  override def getEmployees: ServiceCall[NotUsed, Seq[Employee]] = ServiceCall { _ =>
    employeeRepository.getEmployees.map(_.map(convertEmployeeReadEntityToEmployee))
  }

  override def getEmployee(id: String): ServiceCall[NotUsed, Employee] = ServiceCall { _ =>
    employeeRepository.getEmployee(id).map { e =>
      if (e.isDefined) convertEmployeeReadEntityToEmployee(e.get)
      else {
        val msg = s"No employee found with id = $id."
        log.error(msg)

        throw NotFound(msg)
      }
    }
  }

  override def deleteEmployee(id: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    entityRef(id).ask(DeleteEmployee(id)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def getLeaves(empId: String): ServiceCall[NotUsed, Leaves] = ServiceCall { _ =>
    employeeRepository.getEmployee(empId).map { e =>
      if (e.isDefined) Leaves(e.get.earnedLeaves, e.get.sickLeaves)
      else {
        val msg = s"No employee found with id = $empId."
        log.error(msg)

        throw NotFound(msg)
      }
    }
  }

  override def createIntimation(empId: String): ServiceCall[IntimationReq, Done] = ServiceCall { intimationReq =>
    entityRef(empId).ask(CreateIntimation(empId, intimationReq)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def updateIntimation(empId: String): ServiceCall[IntimationReq, Done] = ServiceCall { intimationReq =>
    entityRef(empId).ask(UpdateIntimation(empId, intimationReq)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def cancelIntimation(empId: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    entityRef(empId).ask(CancelIntimation(empId)).recover {
      case e: InvalidCommandException => throw BadRequest(e.message)
    }
  }

  override def getIntimations(empId: String, month: Option[Int], year: Option[Int]): ServiceCall[NotUsed, List[IntimationRes]] = ServiceCall { _ =>
    val m = if (month.isEmpty) LocalDate.now().getMonthValue else month.get
    val y = if (year.isEmpty) LocalDate.now().getYear else year.get

    intimationRepository.getIntimations(empId, m, y).map(convertToIntimationResponse)
  }

  override def getActiveIntimations: ServiceCall[NotUsed, List[IntimationRes]] = ServiceCall { _ =>
    intimationRepository.getActiveIntimations.map(convertToIntimationResponse)
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
      case EmployeeAdded(id, name, gender, doj, pfn, isActive, leaves) =>
        EmployeeAddedKafkaEvent(id, name, gender, doj, pfn, isActive, leaves)

      case EmployeeTerminated(id, _, _, _, _, _, _) =>
        EmployeeTerminatedKafkaEvent(id)

      case EmployeeDeleted(id) =>
        EmployeeDeletedKafkaEvent(id)

      case IntimationCreated(empId, reason, requests) =>
        IntimationCreatedKafkaEvent(empId, reason, requests)

      case IntimationUpdated(empId, reason, requests) =>
        IntimationUpdatedKafkaEvent(empId, reason, requests)

      case IntimationCancelled(empId, reason, requests) =>
        IntimationCancelledKafkaEvent(empId, reason, requests)
    }
  }

  private def convertEmployeeReadEntityToEmployee(e: EmployeeEntity): Employee = {
    api.model.Employee(e.id, e.name, e.gender, e.doj, e.pfn, e.isActive, Leaves(e.earnedLeaves, e.sickLeaves))
  }

  private def convertToIntimationResponse(s: Seq[(IntimationEntity, RequestEntity)]): List[IntimationRes] = {
    s.groupBy { case (ie, _) => ie.empId }
      .flatMap {
        case (empId, s) =>
          s.groupBy { case (ie, _) => ie }
            .map {
              case (ie, s) =>
                val requests = s.map { case (_, r) => Request(LocalDate.of(r.year, r.month, r.date), r.requestType) }.toSet
                ie.id -> (ie.reason, requests)
            }
            .map {
              case (_, t) => IntimationRes(empId, t._1, t._2)
            }
      }
      .toList
  }

}
