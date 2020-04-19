package com.codingkapoor.employee.impl.persistence.write.models

import java.time.LocalDate

import akka.Done
import com.codingkapoor.employee.api.models.{Employee, EmployeeInfo, IntimationReq, Leaves, PrivilegedIntimation}
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

case class ReleaseEmployee(id: Long, dor: LocalDate) extends EmployeeCommand[Done]

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

case class CreatePrivilegedIntimation(empId: Long, privilegedIntimation: PrivilegedIntimation) extends EmployeeCommand[Leaves]

object CreatePrivilegedIntimation {
  implicit val format: Format[CreatePrivilegedIntimation] = Json.format[CreatePrivilegedIntimation]
}

case class UpdatePrivilegedIntimation(empId: Long, privilegedIntimation: PrivilegedIntimation) extends EmployeeCommand[Leaves]

object UpdatePrivilegedIntimation {
  implicit val format: Format[UpdatePrivilegedIntimation] = Json.format[UpdatePrivilegedIntimation]
}

case class CancelPrivilegedIntimation(empId: Long) extends EmployeeCommand[Leaves]

object CancelPrivilegedIntimation {
  implicit val format: Format[CancelPrivilegedIntimation] = Json.format[CancelPrivilegedIntimation]
}

case class CreditLeaves(empId: Long) extends EmployeeCommand[Done]

object CreditLeaves {
  implicit val format: Format[CreditLeaves] = Json.format[CreditLeaves]
}

case class BalanceLeaves(empId: Long) extends EmployeeCommand[Done]

object BalanceLeaves {
  implicit val format: Format[BalanceLeaves] = Json.format[BalanceLeaves]
}
