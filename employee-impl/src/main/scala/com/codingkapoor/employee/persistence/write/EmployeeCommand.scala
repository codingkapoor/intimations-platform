package com.codingkapoor.employee.persistence.write

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.model.{Employee, IntimationReq}

sealed trait EmployeeCommand[R] extends ReplyType[R]

case class AddEmployee(employee: Employee) extends EmployeeCommand[Done]

object AddEmployee {
  implicit val format: Format[AddEmployee] = Json.format[AddEmployee]
}

case class TerminateEmployee(id: Long) extends EmployeeCommand[Done]

object TerminateEmployee {
  implicit val format: Format[TerminateEmployee] = Json.format[TerminateEmployee]
}

case class DeleteEmployee(id: Long) extends EmployeeCommand[Done]

object DeleteEmployee {
  implicit val format: Format[DeleteEmployee] = Json.format[DeleteEmployee]
}

case class CreateIntimation(empId: Long, intimationReq: IntimationReq) extends EmployeeCommand[Done]

object CreateIntimation {
  implicit val format: Format[CreateIntimation] = Json.format[CreateIntimation]
}

case class UpdateIntimation(empId: Long, intimationReq: IntimationReq) extends EmployeeCommand[Done]

object UpdateIntimation {
  implicit val format: Format[UpdateIntimation] = Json.format[UpdateIntimation]
}

case class CancelIntimation(empId: Long) extends EmployeeCommand[Done]

object CancelIntimation {
  implicit val format: Format[CancelIntimation] = Json.format[CancelIntimation]
}
