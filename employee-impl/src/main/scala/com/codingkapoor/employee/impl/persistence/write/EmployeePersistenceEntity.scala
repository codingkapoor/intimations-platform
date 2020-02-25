package com.codingkapoor.employee.impl.persistence.write

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.LoggerFactory
import com.codingkapoor.employee.api.models.{Employee, Intimation, Leaves, Request, RequestType, Role}
import com.codingkapoor.employee.impl.persistence.write.models._

class EmployeePersistenceEntity extends PersistentEntity {

  private val logger = LoggerFactory.getLogger(classOf[EmployeePersistenceEntity])

  import EmployeePersistenceEntity._

  override type Command = EmployeeCommand[_]
  override type Event = EmployeeEvent
  override type State = Option[EmployeeState]

  override def initialState: Option[EmployeeState] = None

  override def behavior: Behavior = {
    case state if state.isEmpty => initial
    case state if state.isDefined && state.get.dor.isDefined => employeeReleased
    case state if state.isDefined => employeeAdded
  }

  private val initial: Actions =
    Actions()
      .onCommand[AddEmployee, Done] {
        case (AddEmployee(e), ctx, state) =>
          logger.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          ctx.thenPersist(
            EmployeeAdded(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles)
          )(_ => ctx.reply(Done))

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(id, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val msg = s"No employee found with id = $id."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[ReleaseEmployee, Done] {
      case (ReleaseEmployee(id), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ReleaseEmployee command.")

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

    }.onCommand[CreateIntimation, Leaves] {
      case (CreateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdateIntimation, Leaves] {
      case (UpdateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelIntimation, Leaves] {
      case (CancelIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CreatePrerogativeIntimation, Leaves] {
      case (CreatePrerogativeIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreatePrerogativeIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdatePrerogativeIntimation, Leaves] {
      case (UpdatePrerogativeIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdatePrerogativeIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelPrerogativeIntimation, Leaves] {
      case (CancelPrerogativeIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelPrerogativeIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CreditLeaves, Done] {
      case (CreditLeaves(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received Credit command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[BalanceLeaves, Done] {
      case (BalanceLeaves(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received BalanceLeaves command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onEvent {
      case (EmployeeAdded(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles), _) =>
        Some(EmployeeState(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles, None, Leaves()))
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

        ctx.thenPersist(EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles))(_ =>
          ctx.reply(Employee(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles)))

    }.onCommand[ReleaseEmployee, Done] {
      case (ReleaseEmployee(_), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ReleaseEmployee command.")

        if (e.roles.contains(Role.Admin)) {
          val msg = s"Employees (id = ${e.id}) with admin privileges can't be released. Admin privileges must be revoked first."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else ctx.thenPersist(EmployeeReleased(e.id, LocalDate.now()))(_ => ctx.reply(Done))

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        if (e.roles.contains(Role.Admin)) {
          val msg = s"Employees (id = ${e.id}) with admin privileges can't be deleted. Admin privileges must be revoked first."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else ctx.thenPersist(EmployeeDeleted(id))(_ => ctx.reply(Done))

    }.onCommand[CreateIntimation, Leaves] {
      case (CreateIntimation(empId, intimationReq), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val latestRequestDate = activeIntimation.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

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

        } else if (e.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
          val newLeaves = getNewLeaves(intimationReq.requests, lastLeaves = Leaves(e.leaves.earned, e.leaves.currentYearEarned, e.leaves.sick, e.leaves.extra))

          ctx.thenPersistAll(
            IntimationCreated(empId, intimationReq.reason, LocalDateTime.now(), intimationReq.requests),
            LastLeavesSaved(empId, e.leaves.earned, e.leaves.currentYearEarned, e.leaves.sick, e.leaves.extra),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(newLeaves))

        } else {
          val msg = s"Only single active intimation at a given time is supported. Cancel an active intimation first so as to create a new intimation."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        }

    }.onCommand[UpdateIntimation, Leaves] {
      case (UpdateIntimation(empId, intimationReq), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val requests1 = activeIntimation.requests
        lazy val requests2 = intimationReq.requests

        lazy val latestRequestDate = requests1.map(_.date).toList.sortWith(_.isBefore(_)).last

        lazy val requestsAlreadyConsumed = requests1.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
        lazy val newRequestAlreadyConsumed = requests2.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))

        if (intimationReq.requests.isEmpty || intimationReq.reason.trim == "") {
          val msg = "Intimation can't be updated with an empty requests or a empty reason"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done
        } else if (e.activeIntimationOpt.isEmpty) {
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
          val newLeaves = getNewLeaves(intimationReq.requests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

          ctx.thenPersistAll(
            IntimationUpdated(empId, intimationReq.reason, LocalDateTime.now(), newRequests),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(newLeaves))
        }

    }.onCommand[CancelIntimation, Leaves] {
      case (CancelIntimation(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val requests = activeIntimation.requests
        lazy val reason = activeIntimation.reason

        lazy val latestRequestDate = activeIntimation.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (e.activeIntimationOpt.isEmpty) {
          val msg = s"No intimations found."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate))
          ctx.done

        else {
          val requestsAlreadyConsumed = requests.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
          val newLeaves = getNewLeaves(requestsAlreadyConsumed, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

          ctx.thenPersistAll(
            IntimationCancelled(empId, reason, LocalDateTime.now(), requestsAlreadyConsumed),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(newLeaves))
        }

    }.onCommand[CreditLeaves, Done] {
      case (CreditLeaves(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ${CreditLeaves(empId)} command.")

        val (earnedCredits, sickCredits) = computeCredits(e.doj)

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val latestRequestDate = activeIntimation.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (e.activeIntimationOpt.isDefined && latestRequestDate.isAfter(LocalDate.now())) {
          val balanced = balanceExtra(e.lastLeaves.earned + earnedCredits, e.lastLeaves.currentYearEarned + earnedCredits, e.lastLeaves.sick + sickCredits, e.lastLeaves.extra)
          val newLeaves = getNewLeaves(activeIntimation.requests, balanced)

          ctx.thenPersistAll(
            LastLeavesSaved(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            LeavesCredited(e.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(Done))

        } else {
          val balanced = balanceExtra(e.leaves.earned + earnedCredits, e.leaves.currentYearEarned + earnedCredits, e.leaves.sick + sickCredits, e.leaves.extra)
          val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

          ctx.thenPersistAll(
            LastLeavesSaved(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            LeavesCredited(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(Done))
        }

    }.onCommand[BalanceLeaves, Done] {
      case (BalanceLeaves(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ${BalanceLeaves(empId)} command.")

        val totalLeaves = e.leaves.currentYearEarned + e.leaves.sick
        val balancedTotalLeaves = if (totalLeaves > 10) 10 else totalLeaves

        val totalCumulativeLeaves = if (totalLeaves > 10) e.leaves.earned - e.leaves.currentYearEarned + 10 else e.leaves.earned + e.leaves.sick
        val balancedTotalCumulativeLeaves = if (totalCumulativeLeaves > 20) 20 else totalCumulativeLeaves

        val lapsedLeaves = (totalLeaves - balancedTotalLeaves) + (totalCumulativeLeaves - balancedTotalCumulativeLeaves) + e.leaves.extra
        val newLeaves = Leaves(earned = balancedTotalCumulativeLeaves)

        ctx.thenPersistAll(
          LeavesBalanced(e.id, balancedTotalCumulativeLeaves, lapsedLeaves),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
        )(() => ctx.reply(Done))

    }.onEvent {
      case (EmployeeUpdated(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles), Some(e)) =>
        Some(EmployeeState(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles, e.activeIntimationOpt, e.lastLeaves))

      case (EmployeeReleased(_, dor), Some(e)) =>
        Some(e.copy(dor = Some(dor)))

      case (EmployeeDeleted(_), _) =>
        None

      case (IntimationCreated(_, reason, lastModified, requests), Some(e)) =>
        Some(e.copy(activeIntimationOpt = Some(Intimation(reason, lastModified, requests))))

      case (IntimationUpdated(_, reason, lastModified, requests), Some(e)) =>
        Some(e.copy(activeIntimationOpt = Some(Intimation(reason, lastModified, requests))))

      case (IntimationCancelled(_, _, _, _), Some(e)) =>
        Some(e.copy(activeIntimationOpt = None))

      case (LastLeavesSaved(_, earned, currentYearEarned, sick, extra), Some(e)) =>
        Some(e.copy(lastLeaves = Leaves(earned, currentYearEarned, sick, extra)))

      case (LeavesCredited(_, earned, currentYearEarned, sick, extra), Some(e)) =>
        Some(e.copy(leaves = Leaves(earned, currentYearEarned, sick, extra)))

      case (LeavesBalanced(_, earned, _), Some(e)) =>
        Some(e.copy(leaves = Leaves(earned)))
    }

  private val employeeReleased: Actions =
    Actions()
      .onCommand[AddEmployee, Done] {
        case (AddEmployee(e), ctx, state) =>
          logger.info(s"EmployeePersistenceEntity at state = $state received AddEmployee command.")

          val msg = s"Employee with id = ${e.id} already exists."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

      }.onCommand[UpdateEmployee, Employee] {
      case (UpdateEmployee(id, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateEmployee command.")

        val msg = s"Employee with id = $id has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[ReleaseEmployee, Done] {
      case (ReleaseEmployee(id), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ReleaseEmployee command.")

        val msg = s"Employee with id = $id has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[DeleteEmployee, Done] {
      case (DeleteEmployee(id), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received DeleteEmployee command.")

        ctx.thenPersist(EmployeeDeleted(id))(_ => ctx.reply(Done))

    }.onCommand[CreateIntimation, Leaves] {
      case (CreateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreateIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdateIntimation, Leaves] {
      case (UpdateIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdateIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelIntimation, Leaves] {
      case (CancelIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CreatePrerogativeIntimation, Leaves] {
      case (CreatePrerogativeIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreatePrerogativeIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdatePrerogativeIntimation, Leaves] {
      case (UpdatePrerogativeIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdatePrerogativeIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelPrerogativeIntimation, Leaves] {
      case (CancelPrerogativeIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelPrerogativeIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CreditLeaves, Done] {
      case (CreditLeaves(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received Credit command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[BalanceLeaves, Done] {
      case (BalanceLeaves(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received BalanceLeaves command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onEvent {
      case (EmployeeDeleted(_), _) =>
        None
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
    val currentYearEarned = lastLeaves.currentYearEarned
    val sick = lastLeaves.sick
    val extra = lastLeaves.extra

    val applied = getTotalNumOfLeavesApplied(requests)

    if (sick >= applied)
      Leaves(earned = earned, currentYearEarned = currentYearEarned, sick = sick - applied)
    else {
      if (earned >= (applied - sick))
        Leaves(
          earned = earned - (applied - sick),
          currentYearEarned = if (currentYearEarned - (applied - sick) < 0) 0 else currentYearEarned - (applied - sick)
        )
      else
        Leaves(extra = extra + applied - (earned + sick))
    }
  }

  private def balanceExtra(earned: Double, currentYearEarned: Double, sick: Double, due: Double): Leaves = {
    if (sick >= due)
      Leaves(earned = earned, currentYearEarned = currentYearEarned, sick = sick - due)
    else {
      if (earned >= (due - sick))
        Leaves(
          earned = earned - (due - sick),
          currentYearEarned = if (currentYearEarned - (due - sick) < 0) 0 else currentYearEarned - (due - sick)
        )
      else
        Leaves(extra = due - (earned + sick))
    }
  }

  private def computeCredits(doj: LocalDate): (Double, Double) = {
    val today = LocalDate.now()

    if (today.getMonthValue == doj.getMonthValue && today.getYear == doj.getYear) {
      if (today.getDayOfMonth - doj.getDayOfMonth >= 15) (1.5, 0.5)
      else if (today.getDayOfMonth - doj.getDayOfMonth >= 10) (1.0, 0.0)
      else (0.0, 0.0)
    } else (1.5, 0.5)
  }

}
