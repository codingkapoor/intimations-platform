package com.codingkapoor.employee.service

import akka.{Done, NotUsed}
import com.codingkapoor.employee.api
import com.codingkapoor.employee.api.{Employee, EmployeeService}
import com.codingkapoor.employee.persistence.read.{EmployeeEntity, EmployeeRepository}
import com.codingkapoor.employee.persistence.write._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class EmployeeServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeRepository) extends EmployeeService {

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

  override def employeeTopic: Topic[api.EmployeeKafkaEvent] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(EmployeeEvent.Tag, fromOffset)
        .map(event => (convertPersistentEntityEventToKafkaEvent(event), event.offset))
    }
  }
}

object EmployeeServiceImpl {

  private def convertPersistentEntityEventToKafkaEvent(eventStreamElement: EventStreamElement[EmployeeEvent]): api.EmployeeKafkaEvent = {
  eventStreamElement.event match {
    case EmployeeAdded(id, name, gender, doj, pfn, isActive) => api.EmployeeAddedKafkaEvent(id, name, gender, doj, pfn, isActive)
    case EmployeeTerminated(id, _, _, _, _, _) => api.EmployeeTerminatedKafkaEvent(id)
    case EmployeeDeleted(id) => api.EmployeeDeletedKafkaEvent(id)
  }
}
  private def convertEmployeeReadEntityToEmployee(e: EmployeeEntity): api.Employee = {
    api.Employee(e.id, e.name, e.gender, e.doj, e.pfn, e.isActive)
  }
}
