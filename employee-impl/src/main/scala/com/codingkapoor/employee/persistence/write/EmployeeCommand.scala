package com.codingkapoor.employee.persistence.write

import akka.Done
import com.codingkapoor.employee.api.model.Employee
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}

sealed trait EmployeeCommand[R] extends ReplyType[R]

case class AddEmployee(employee: Employee) extends EmployeeCommand[Done]

object AddEmployee {
  implicit val format: Format[AddEmployee] = Json.format[AddEmployee]
}

case class TerminateEmployee(id: String) extends EmployeeCommand[Done]

object TerminateEmployee {
  implicit val format: Format[TerminateEmployee] = Json.format[TerminateEmployee]
}

case object GetEmployees extends EmployeeCommand[List[Employee]] {
  implicit val format: Format[GetEmployees.type] = Json.format[GetEmployees.type]
}

case class GetEmployee(id: String) extends EmployeeCommand[Employee]

object GetEmployee {
  implicit val format: Format[GetEmployee] = Json.format[GetEmployee]
}

case class DeleteEmployee(id: String) extends EmployeeCommand[Done]

object DeleteEmployee {
  implicit val format: Format[DeleteEmployee] = Json.format[DeleteEmployee]
}
