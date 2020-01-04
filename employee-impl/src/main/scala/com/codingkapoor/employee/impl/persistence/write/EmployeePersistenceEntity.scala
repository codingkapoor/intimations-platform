package com.codingkapoor.employee.impl.persistence.write

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.LoggerFactory

import com.codingkapoor.employee.api.model.{Employee, Intimation}

class EmployeePersistenceEntity extends PersistentEntity {

  private val log = LoggerFactory.getLogger(classOf[EmployeePersistenceEntity])

  private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  private def already5(date: LocalDate) = LocalDateTime.now().isAfter(LocalDateTime.parse(date.toString + " 05:00", dtf))

  override type Command = EmployeeCommand[_]
  override type Event = EmployeeEvent
  override type State = Option[EmployeeState]

  override def initialState: Option[EmployeeState] = None

  override def behavior: Behavior = {
    case state if state.isEmpty => initial
    case state if state.isDefined => employeeAdded
  }

  private val initial: Actions =
    Actions()
      .onCommand[AddEmployee, Done] {
        case (AddEmployee(e), ctx, state) =>
          log.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          ctx.thenPersist(
            EmployeeAdded(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location, e.leaves, e.roles)
          )(_ => ctx.reply(Done))

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(_), ctx, state@Some(e)) =>
        log.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val msg = s"No employee found with id = ${e.id}."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onCommand[TerminateEmployee, Done] {
      case (TerminateEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")

        val msg = s"No employee found with id = $id."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        val msg = s"No employee found with id = $id."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onCommand[CreateIntimation, Done] {
      case (CreateIntimation(empId, _), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        val msg = s"No employee found with id = $empId."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onCommand[UpdateIntimation, Done] {
      case (UpdateIntimation(empId, _), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        val msg = s"No employee found with id = $empId."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onCommand[CancelIntimation, Done] {
      case (CancelIntimation(empId), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val msg = s"No employee found with id = $empId."
        ctx.invalidCommand(msg)

        log.info(s"InvalidCommandException: $msg")
        ctx.done

    }.onEvent {
      case (EmployeeAdded(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles), _) =>
        Some(EmployeeState(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles, Nil))
    }

  private val employeeAdded: Actions =
    Actions()
      .onCommand[AddEmployee, Done] {
        case (AddEmployee(e), ctx, state) =>
          log.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          val msg = s"Employee with id = ${e.id} already exists."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(employeeInfo), ctx, state@Some(e)) =>
        log.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val designation = employeeInfo.designation.getOrElse(e.designation)
        val contactInfo = employeeInfo.contactInfo.getOrElse(e.contactInfo)
        val location = employeeInfo.location.getOrElse(e.location)
        val leaves = employeeInfo.leaves.getOrElse(e.leaves)
        val roles = employeeInfo.roles.getOrElse(e.roles)

        val employee = Employee(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, leaves, roles)

        ctx.thenPersist(
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, leaves, roles)
        )(_ => ctx.reply(employee))

    }.onCommand[TerminateEmployee, Done] {
      case (TerminateEmployee(_), ctx, state@Some(e)) =>
        log.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")
        ctx.thenPersist(
          EmployeeTerminated(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, isActive = false, e.contactInfo, e.location, e.leaves, e.roles)
        )(_ => ctx.reply(Done))

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")
        ctx.thenPersist(EmployeeDeleted(id))(_ => ctx.reply(Done))

    }.onCommand[CreateIntimation, Done] {
      case (CreateIntimation(empId, intimationReq), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        val intimations = state.get.intimations
        lazy val latestRequestDate = intimations.head.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (intimationReq.requests.exists(_.date.isBefore(LocalDate.now())) || intimationReq.requests.exists(r => already5(r.date))) {
          val msg = s"Intimation can't be created for dates in the past."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

        } else if (intimations.isEmpty || latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate))
          ctx.thenPersist(IntimationCreated(empId, intimationReq.reason, LocalDateTime.now(), intimationReq.requests))(_ => ctx.reply(Done))

        else {
          val msg = s"System only supports single active intimation at a given time. Cancel an active intimation first so as to create a new intimation."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done
        }

    }.onCommand[UpdateIntimation, Done] {
      case (UpdateIntimation(empId, intimationReq), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        val intimations = state.get.intimations
        lazy val requests1 = intimations.head.requests
        lazy val requests2 = intimationReq.requests

        lazy val latestRequestDate = requests1.map(_.date).toList.sortWith(_.isBefore(_)).last

        lazy val requestsAlreadyConsumed = requests1.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
        lazy val newRequestAlreadyConsumed = requests2.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))

        if (intimations.isEmpty) {
          val msg = s"No intimations found."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

        } else if (latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
          val msg = s"No active intimations found to update."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

        } else if (!(requestsAlreadyConsumed equals newRequestAlreadyConsumed)) {
          val msg = s"Dates in past can't be modified."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

        } else {
          val newRequests = requestsAlreadyConsumed ++ requests2
          ctx.thenPersist(IntimationUpdated(empId, intimationReq.reason, LocalDateTime.now(), newRequests))(_ => ctx.reply(Done))
        }

    }.onCommand[CancelIntimation, Done] {
      case (CancelIntimation(empId), ctx, state) =>
        log.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val intimations = state.get.intimations
        lazy val requests = intimations.head.requests
        lazy val reason = intimations.head.reason

        lazy val latestRequestDate = intimations.head.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (intimations.isEmpty) {
          val msg = s"No intimations found."
          ctx.invalidCommand(msg)

          log.info(s"InvalidCommandException: $msg")
          ctx.done

        } else if (latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate))
          ctx.done

        else {
          val requestsAlreadyConsumed = requests.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
          ctx.thenPersist(IntimationCancelled(empId, reason, LocalDateTime.now(), requestsAlreadyConsumed))(_ => ctx.reply(Done))
        }

    }.onEvent {
      case (EmployeeUpdated(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles), state) =>
        Some(EmployeeState(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles, state.get.intimations))

      case (EmployeeTerminated(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles), state) =>
        Some(EmployeeState(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles, state.get.intimations))

      case (EmployeeDeleted(_), _) =>
        None

      case (IntimationCreated(_, reason, lastModified, requests), Some(e)) =>
        val intimations = Intimation(reason, lastModified, requests) :: e.intimations
        Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location, e.leaves, e.roles, intimations))

      case (IntimationUpdated(_, reason, lastModified, requests), Some(e)) =>
        val intimations = Intimation(reason, lastModified, requests) :: e.intimations.tail
        Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location, e.leaves, e.roles, intimations))

      case (IntimationCancelled(_, reason, lastModified, requests), Some(e)) =>
        val intimations = if (requests.isEmpty) e.intimations.tail else Intimation(reason, lastModified, requests) :: e.intimations.tail
        Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location, e.leaves, e.roles, intimations))

    }
}
