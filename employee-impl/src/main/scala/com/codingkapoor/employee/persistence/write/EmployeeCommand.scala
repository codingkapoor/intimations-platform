package com.codingkapoor.employee.persistence.write

import akka.Done
import com.codingkapoor.employee.api.Employee
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}

sealed trait EmployeeCommand[R] extends ReplyType[R]

case class AddEmployee(employee: Employee) extends EmployeeCommand[Done]

object AddEmployee {
  implicit val format: Format[AddEmployee] = Json.format[AddEmployee]
}

case object GetEmployees extends EmployeeCommand[List[Employee]] {
  implicit val format: Format[GetEmployees.type] = Json.format[GetEmployees.type]
}
