package com.codingkapoor.employee.impl.persistence.write.models

import akka.Done
import com.codingkapoor.employee.api.models.{Employee, EmployeeInfo, IntimationReq, Leaves, PrerogativeIntimation}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}

sealed trait EmployeeCommand[R] extends ReplyType[R]

case class AddEmployee(employee: Employee) extends EmployeeCommand[Done]

object AddEmployee {
  implicit val format: Format[AddEmployee] = Json.format[AddEmployee]
}

case class UpdateEmployee(id: Long, employeeInfo: EmployeeInfo) extends EmployeeCommand[Employee]

object UpdateEmployee {
  implicit val format: Format[UpdateEmployee] = Json.format[UpdateEmployee]
}

case class ReleaseEmployee(id: Long) extends EmployeeCommand[Done]

object ReleaseEmployee {
  implicit val format: Format[ReleaseEmployee] = Json.format[ReleaseEmployee]
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

case class CreatePrerogativeIntimation(empId: Long, prerogativeIntimation: PrerogativeIntimation) extends EmployeeCommand[Leaves]

object CreatePrerogativeIntimation {
  implicit val format: Format[CreatePrerogativeIntimation] = Json.format[CreatePrerogativeIntimation]
}

case class UpdatePrerogativeIntimation(empId: Long, prerogativeIntimation: PrerogativeIntimation) extends EmployeeCommand[Leaves]

object UpdatePrerogativeIntimation {
  implicit val format: Format[UpdatePrerogativeIntimation] = Json.format[UpdatePrerogativeIntimation]
}

case class CancelPrerogativeIntimation(empId: Long) extends EmployeeCommand[Leaves]

object CancelPrerogativeIntimation {
  implicit val format: Format[CancelPrerogativeIntimation] = Json.format[CancelPrerogativeIntimation]
}

case class CreditLeaves(empId: Long) extends EmployeeCommand[Done]

object CreditLeaves {
  implicit val format: Format[CreditLeaves] = Json.format[CreditLeaves]
}

case class BalanceLeaves(empId: Long) extends EmployeeCommand[Done]

object BalanceLeaves {
  implicit val format: Format[BalanceLeaves] = Json.format[BalanceLeaves]
}
