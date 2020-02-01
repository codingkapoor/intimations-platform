package com.codingkapoor.employee.impl.persistence.write

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}
import com.codingkapoor.employee.api.model.{Employee, EmployeeInfo, IntimationReq, Leaves}

sealed trait EmployeeCommand[R] extends ReplyType[R]

case class AddEmployee(employee: Employee) extends EmployeeCommand[Done]

object AddEmployee {
  implicit val format: Format[AddEmployee] = Json.format[AddEmployee]
}

case class UpdateEmployee(id: Long, employeeInfo: EmployeeInfo) extends EmployeeCommand[Employee]

object UpdateEmployee {
  implicit val format: Format[UpdateEmployee] = Json.format[UpdateEmployee]
}

case class TerminateEmployee(id: Long) extends EmployeeCommand[Done]

object TerminateEmployee {
  implicit val format: Format[TerminateEmployee] = Json.format[TerminateEmployee]
}

case class DeleteEmployee(id: Long) extends EmployeeCommand[Done]

object DeleteEmployee {
  implicit val format: Format[DeleteEmployee] = Json.format[DeleteEmployee]
}

case class CreateIntimation(empId: Long, intimationReq: IntimationReq) extends EmployeeCommand[Leaves]

object CreateIntimation {
  implicit val format: Format[CreateIntimation] = Json.format[CreateIntimation]
}

case class UpdateIntimation(empId: Long, intimationReq: IntimationReq) extends EmployeeCommand[Leaves]

object UpdateIntimation {
  implicit val format: Format[UpdateIntimation] = Json.format[UpdateIntimation]
}

case class CancelIntimation(empId: Long) extends EmployeeCommand[Leaves]

object CancelIntimation {
  implicit val format: Format[CancelIntimation] = Json.format[CancelIntimation]
}

case class Credit(empId: Long) extends EmployeeCommand[Done]

object Credit {
  implicit val format: Format[Credit] = Json.format[Credit]
}
