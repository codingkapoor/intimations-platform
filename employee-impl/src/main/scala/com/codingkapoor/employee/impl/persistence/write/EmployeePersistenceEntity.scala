package com.codingkapoor.employee.impl.persistence.write

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.LoggerFactory
import com.codingkapoor.employee.api.model.{Employee, EmployeeInfo, Intimation, Leaves, Request, RequestType, Role}

class EmployeePersistenceEntity extends PersistentEntity {

  private val logger = LoggerFactory.getLogger(classOf[EmployeePersistenceEntity])

  import EmployeePersistenceEntity._

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
          logger.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          ctx.thenPersist(
            EmployeeAdded(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location, e.leaves, e.roles)
          )(_ => ctx.reply(Done))

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(id, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val msg = s"No employee found with id = $id."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[TerminateEmployee, Done] {
      case (TerminateEmployee(id), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")

        val msg = s"No employee found with id = $id."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        val msg = s"No employee found with id = $id."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CreateIntimation, Done] {
      case (CreateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdateIntimation, Done] {
      case (UpdateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelIntimation, Done] {
      case (CancelIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onEvent {
      case (EmployeeAdded(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles), _) =>
        Some(EmployeeState(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles, Nil, Leaves()))
    }

  private val employeeAdded: Actions =
    Actions()
      .onCommand[AddEmployee, Done] {
        case (AddEmployee(e), ctx, state) =>
          logger.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          val msg = s"Employee with id = ${e.id} already exists."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(id, employeeInfo), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val designation = employeeInfo.designation.getOrElse(e.designation)
        val contactInfo = employeeInfo.contactInfo.getOrElse(e.contactInfo)
        val location = employeeInfo.location.getOrElse(e.location)
        val roles = employeeInfo.roles.getOrElse(e.roles)

        if (e.intimations.nonEmpty && employeeInfo.leaves.isDefined && employeeInfo.leaves.get != e.leaves) {
          val leaves = employeeInfo.leaves.get

          lazy val latestRequestDate = e.intimations.head.requests.map(_.date).toList.sortWith(_.isBefore(_)).last
          if (latestRequestDate.isAfter(LocalDate.now())) {
            val balanced = balanceExtra(leaves.earned, leaves.sick, e.lastLeaves.extra)
            val newLeaves = getNewLeaves(e.intimations.head.requests, balanced)

            ctx.thenPersistAll(
              LastLeavesSaved(e.id, balanced.earned, balanced.sick, balanced.extra),
              EmployeeUpdated(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, newLeaves, roles)
            )(() => ctx.reply(Employee(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, newLeaves, roles)))
          } else {
            val balanced = balanceExtra(leaves.earned, leaves.sick, e.leaves.extra)

            ctx.thenPersistAll(EmployeeUpdated(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, balanced, roles))(() =>
              ctx.reply(Employee(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, balanced, roles)))
          }
        } else
          ctx.thenPersist(EmployeeUpdated(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, employeeInfo.leaves.getOrElse(e.leaves), roles))(_ =>
            ctx.reply(Employee(e.id, e.name, e.gender, e.doj, designation, e.pfn, e.isActive, contactInfo, location, employeeInfo.leaves.getOrElse(e.leaves), roles)))

    }.onCommand[TerminateEmployee, Done] {
      case (TerminateEmployee(_), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received TerminateEmployee command.")

        if (e.roles.contains(Role.Admin)) {
          val msg = s"Employees (id = ${e.id}) with admin privileges can't be terminated. Admin privileges must be revoked first."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        } else ctx.thenPersist(EmployeeTerminated(e.id))(_ => ctx.reply(Done))

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        if (e.roles.contains(Role.Admin)) {
          val msg = s"Employees (id = ${e.id}) with admin privileges can't be deleted. Admin privileges must be revoked first."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        } else ctx.thenPersist(EmployeeDeleted(id))(_ => ctx.reply(Done))

    }.onCommand[CreateIntimation, Done] {
      case (CreateIntimation(empId, intimationReq), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        lazy val intimations = state.get.intimations
        lazy val latestRequestDate = intimations.head.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (intimationReq.requests.isEmpty || intimationReq.reason.trim == "") {
          val msg = "Intimation can't be created without any requests or a reason"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        } else if (intimationReq.requests.exists(_.date.isBefore(LocalDate.now())) || intimationReq.requests.exists(r => already5(r.date))) {
          val msg = s"Intimation can't be created for dates in the past"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (intimationReq.requests.exists(r => isWeekend(r.date))) {
          val msg = s"Intimation can't be created for weekends"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (intimations.isEmpty || latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
          ctx.thenPersistAll(
            IntimationCreated(empId, intimationReq.reason, LocalDateTime.now(), intimationReq.requests),
            LastLeavesSaved(empId, e.leaves.earned, e.leaves.sick, e.leaves.extra),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location,
              getNewLeaves(intimationReq.requests, lastLeaves = Leaves(e.leaves.earned, e.leaves.sick, e.leaves.extra)), e.roles)
          )(() => ctx.reply(Done))

        } else {
          val msg = s"Only single active intimation at a given time is supported. Cancel an active intimation first so as to create a new intimation."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        }

    }.onCommand[UpdateIntimation, Done] {
      case (UpdateIntimation(empId, intimationReq), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        lazy val intimations = state.get.intimations
        lazy val requests1 = intimations.head.requests
        lazy val requests2 = intimationReq.requests

        lazy val latestRequestDate = requests1.map(_.date).toList.sortWith(_.isBefore(_)).last

        lazy val requestsAlreadyConsumed = requests1.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
        lazy val newRequestAlreadyConsumed = requests2.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))

        if (intimationReq.requests.isEmpty || intimationReq.reason.trim == "") {
          val msg = "Intimation can't be updated with an empty requests or a empty reason"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        } else if (intimations.isEmpty) {
          val msg = s"No intimations found."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
          val msg = s"No active intimations found to update."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (!(requestsAlreadyConsumed equals newRequestAlreadyConsumed)) {
          val msg = s"Dates in past can't be modified."
          ctx.invalidCommand(msg)

          logger.error(s"InvalidCommandException: $msg")
          ctx.done

        } else {
          val newRequests = requestsAlreadyConsumed ++ requests2
          ctx.thenPersistAll(
            IntimationUpdated(empId, intimationReq.reason, LocalDateTime.now(), newRequests),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location,
              getNewLeaves(intimationReq.requests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.sick, e.lastLeaves.extra)), e.roles)
          )(() => ctx.reply(Done))
        }

    }.onCommand[CancelIntimation, Done] {
      case (CancelIntimation(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val intimations = state.get.intimations
        lazy val requests = intimations.head.requests
        lazy val reason = intimations.head.reason

        lazy val latestRequestDate = intimations.head.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (intimations.isEmpty) {
          val msg = s"No intimations found."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate))
          ctx.done

        else {
          val requestsAlreadyConsumed = requests.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
          ctx.thenPersistAll(
            IntimationCancelled(empId, reason, LocalDateTime.now(), requestsAlreadyConsumed),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.designation, e.pfn, e.isActive, e.contactInfo, e.location,
              getNewLeaves(requestsAlreadyConsumed, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.sick, e.lastLeaves.extra)), e.roles)
          )(() => ctx.reply(Done))
        }

    }.onEvent {
      case (EmployeeUpdated(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles), Some(e)) =>
        Some(EmployeeState(id, name, gender, doj, designation, pfn, isActive, contactInfo, location, leaves, roles, e.intimations, e.lastLeaves))

      case (EmployeeTerminated(_), Some(e)) =>
        Some(e.copy(isActive = false))

      case (EmployeeDeleted(_), _) =>
        None

      case (IntimationCreated(_, reason, lastModified, requests), Some(e)) =>
        val intimations = Intimation(reason, lastModified, requests) :: e.intimations
        Some(e.copy(intimations = intimations))

      case (IntimationUpdated(_, reason, lastModified, requests), Some(e)) =>
        val intimations = Intimation(reason, lastModified, requests) :: e.intimations.tail
        Some(e.copy(intimations = intimations))

      case (IntimationCancelled(_, reason, lastModified, requests), Some(e)) =>
        val intimations = if (requests.isEmpty) e.intimations.tail else Intimation(reason, lastModified, requests) :: e.intimations.tail
        Some(e.copy(intimations = intimations))

      case (LastLeavesSaved(_, earned, sick, extra), Some(e)) =>
        Some(e.copy(lastLeaves = Leaves(earned, sick, extra)))
    }
}

object EmployeePersistenceEntity {
  private def isWeekend(date: LocalDate) = date.getDayOfWeek.toString == "SATURDAY" || date.getDayOfWeek.toString == "SUNDAY"

  private def already5(date: LocalDate): Boolean = {
    def dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    LocalDateTime.now().isAfter(LocalDateTime.parse(date.toString + " 17:00", dtf))
  }

  private def getNewLeaves(requests: Set[Request], lastLeaves: Leaves): Leaves = {
    def getTotalNumOfLeavesApplied(requests: Set[Request]): Double = {
      (requests.count(r => r.firstHalf == RequestType.Leave) * 0.5) +
        (requests.count(r => r.secondHalf == RequestType.Leave) * 0.5)
    }

    val earned = lastLeaves.earned
    val sick = lastLeaves.sick
    val extra = lastLeaves.extra

    val applied = getTotalNumOfLeavesApplied(requests)

    if (sick >= applied)
      Leaves(earned = earned, sick = sick - applied)
    else {
      if (earned >= (applied - sick))
        Leaves(earned = earned - (applied - sick))
      else
        Leaves(extra = extra + applied - (earned + sick))
    }
  }

  private def balanceExtra(earned: Double, sick: Double, due: Double): Leaves = {
    if (sick >= due)
      Leaves(earned = earned, sick = sick - due)
    else {
      if (earned >= (due - sick))
        Leaves(earned = earned - (due - sick))
      else
        Leaves(extra = due - (earned + sick))
    }
  }

}
