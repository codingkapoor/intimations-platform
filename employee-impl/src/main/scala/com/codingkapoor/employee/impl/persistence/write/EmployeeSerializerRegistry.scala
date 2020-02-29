package com.codingkapoor.employee.impl.persistence.write

import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, BalanceLeaves, CancelIntimation, CancelPrivilegedIntimation, CreateIntimation, CreatePrivilegedIntimation, CreditLeaves, DeleteEmployee, EmployeeAdded, EmployeeDeleted, EmployeeReleased, EmployeeState, EmployeeUpdated, IntimationCancelled, IntimationCreated, IntimationUpdated, LastLeavesSaved, LeavesBalanced, LeavesCredited, PrivilegedIntimationCancelled, PrivilegedIntimationCreated, PrivilegedIntimationUpdated, ReleaseEmployee, UpdateEmployee, UpdateIntimation, UpdatePrivilegedIntimation}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import scala.collection.immutable.Seq

object EmployeeSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[EmployeeState],
    JsonSerializer[AddEmployee],
    JsonSerializer[EmployeeAdded],
    JsonSerializer[UpdateEmployee],
    JsonSerializer[EmployeeUpdated],
    JsonSerializer[ReleaseEmployee],
    JsonSerializer[EmployeeReleased],
    JsonSerializer[DeleteEmployee],
    JsonSerializer[EmployeeDeleted],
    JsonSerializer[CreateIntimation],
    JsonSerializer[IntimationCreated],
    JsonSerializer[UpdateIntimation],
    JsonSerializer[IntimationUpdated],
    JsonSerializer[CancelIntimation],
    JsonSerializer[IntimationCancelled],
    JsonSerializer[CreatePrivilegedIntimation],
    JsonSerializer[PrivilegedIntimationCreated],
    JsonSerializer[UpdatePrivilegedIntimation],
    JsonSerializer[PrivilegedIntimationUpdated],
    JsonSerializer[CancelPrivilegedIntimation],
    JsonSerializer[PrivilegedIntimationCancelled],
    JsonSerializer[LastLeavesSaved],
    JsonSerializer[CreditLeaves],
    JsonSerializer[LeavesCredited],
    JsonSerializer[BalanceLeaves],
    JsonSerializer[LeavesBalanced]
  )
}
