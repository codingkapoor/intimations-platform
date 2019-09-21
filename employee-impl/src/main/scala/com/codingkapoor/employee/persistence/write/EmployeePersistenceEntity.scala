package com.codingkapoor.employee.persistence.write

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.LoggerFactory

class EmployeePersistenceEntity extends PersistentEntity {

  private val log = LoggerFactory.getLogger(classOf[EmployeePersistenceEntity])

  override type Command = EmployeeCommand[_]
  override type Event = EmployeeEvent
  override type State = Option[EmployeeState]

  override def initialState: Option[EmployeeState] = None

  override def behavior: Behavior = {
    case state if state.isEmpty => initial
    case state if state.isDefined => employeeAdded
  }

  private val initial: Actions =
    Actions().onCommand[AddEmployee, Done] {
      case (AddEmployee(e), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

        ctx.thenPersist(EmployeeAdded(e.id, e.name, e.gender, e.doj, e.pfn, e.isActive)) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[TerminateEmployee, Done]{
      case(TerminateEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")

        val msg = s"No employee found with id = ${id}."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")

        ctx.done
    }.onCommand[DeleteEmployee, Done] {
      case(DeleteEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        val msg = s"No employee found with id = ${id}."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")

        ctx.done
    }.onEvent {
      case (EmployeeAdded(id, name, gender, doj, pfn, isActive), _) =>
        Some(EmployeeState(id, name, gender, doj, pfn, isActive))
    }

  private val employeeAdded: Actions =
    Actions().onCommand[AddEmployee, Done]{
      case (AddEmployee(e), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

        val msg = s"Employee with id = ${e.id} already exists."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")

        ctx.done
    }.onCommand[TerminateEmployee, Done] {
      case (TerminateEmployee(_), ctx, state @ Some(employee)) =>
        log.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")

        ctx.thenPersist(EmployeeTerminated(employee.id, employee.name, employee.gender, employee.doj, employee.pfn, isActive = false)) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        ctx.thenPersist(EmployeeDeleted(id)) { _ =>
          ctx.reply(Done)
        }
    }.onEvent {
      case (EmployeeTerminated(id, name, gender, doj, pfn, isActive), _) =>
        Some(EmployeeState(id, name, gender, doj, pfn, isActive))
      case (EmployeeDeleted(_), _) =>
        None
    }
}
