package com.codingkapoor.employee.service

import akka.{Done, NotUsed}
import com.codingkapoor.employee.api.{Employee, EmployeeServiceApi}
import com.codingkapoor.employee.persistence.read.EmployeeRepository
import com.codingkapoor.employee.persistence.write.{AddEmployee, EmployeePersistenceEntity, UpdateEmployee}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

class EmployeeService(persistentEntityRegistry: PersistentEntityRegistry, employeeRepository: EmployeeRepository) extends EmployeeServiceApi {
  private def entityRef(id: String) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id)

  override def addEmployee(): ServiceCall[Employee, Done] = ServiceCall { employee =>
    entityRef(employee.id).ask(AddEmployee(employee))
  }

  override def getEmployee(id: String): ServiceCall[NotUsed, Employee] = ServiceCall { _ =>
    employeeRepository.getEmployee(id)
  }

  override def getEmployees: ServiceCall[NotUsed, Seq[Employee]] = ServiceCall { _ =>
    employeeRepository.getEmployees
  }

  override def updateEmployee(id: String): ServiceCall[Employee, Done] = { employee =>
    entityRef(employee.id).ask(UpdateEmployee(employee))
  }
}
