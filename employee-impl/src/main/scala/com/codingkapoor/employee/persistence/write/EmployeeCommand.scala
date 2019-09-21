package com.codingkapoor.employee.persistence.write

import akka.Done
import com.codingkapoor.employee.api.model.{Employee, Leaves}
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

case class DeleteEmployee(id: String) extends EmployeeCommand[Done]

object DeleteEmployee {
  implicit val format: Format[DeleteEmployee] = Json.format[DeleteEmployee]
}
