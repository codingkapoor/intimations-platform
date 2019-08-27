package com.codingkapoor.employee.service

import akka.{Done, NotUsed}
import com.codingkapoor.employee.api
import com.codingkapoor.employee.api.{Employee, EmployeeService}
import com.codingkapoor.employee.persistence.read.EmployeeRepository
import com.codingkapoor.employee.persistence.write._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

class EmployeeServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeRepository) extends EmployeeService {
  private def entityRef(id: String) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id)

  override def addEmployee(): ServiceCall[Employee, Done] = ServiceCall { employee =>
    entityRef(employee.id).ask(AddEmployee(employee))
  }

  override def getEmployees: ServiceCall[NotUsed, Seq[Employee]] = ServiceCall { _ =>
    employeeRepository.getEmployees
  }

  override def employeeTopic: Topic[api.EmployeeAddedEvent] = {
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry.eventStream(EmployeeEvent.Tag, fromOffset)
        .map(event => (convertEvent(event), event.offset))
    }
  }

  private def convertEvent(eventStreamElement: EventStreamElement[EmployeeEvent]): api.EmployeeAddedEvent = {
    eventStreamElement.event match {
      case EmployeeAdded(id, name, gender, doj, pfn) => api.EmployeeAddedEvent(id, name, gender, doj, pfn)
    }
  }
}
