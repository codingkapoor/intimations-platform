package com.codingkapoor.employee.impl.persistence.write

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import org.slf4j.LoggerFactory
import com.codingkapoor.employee.api.models.{Employee, Intimation, Leaves, PrivilegedIntimation, PrivilegedIntimationType, Request, RequestType, Role}
import com.codingkapoor.employee.api.models.PrivilegedIntimationType._
import com.codingkapoor.employee.impl.persistence.write.models.{EmployeeReleased, LastLeavesSaved, _}

import scala.collection.immutable

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

    }.onCommand[CreatePrivilegedIntimation, Leaves] {
      case (CreatePrivilegedIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreatePrivilegedIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdatePrivilegedIntimation, Leaves] {
      case (UpdatePrivilegedIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdatePrivilegedIntimation command.")

        val msg = s"No employee found with id = $empId."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelPrivilegedIntimation, Leaves] {
      case (CancelPrivilegedIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelPrivilegedIntimation command.")

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
        Some(EmployeeState(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles, None, None, Leaves()))
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

        } else {
          var newState = e
          val today = LocalDate.now()

          val eventsToPersist: List[EmployeeEvent] =
            if (e.activeIntimationOpt.isDefined || e.privilegedIntimationOpt.isDefined) {
              (if (e.activeIntimationOpt.isDefined) {
                val reason = e.activeIntimationOpt.get.reason
                val requests = e.activeIntimationOpt.get.requests

                val orderedRequests = requests.map(_.date).toList.sortWith(_.isBefore(_))
                val firstRequestDate = orderedRequests.head
                val latestRequestDate = orderedRequests.last

                if ((firstRequestDate.isBefore(today) || firstRequestDate.isEqual(today)) && latestRequestDate.isAfter(today)) {
                  val newRequests = requests.filterNot(r => r.date.isAfter(today))
                  val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

                  val now = LocalDateTime.now()
                  newState = e.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(reason, newRequests, now)))

                  logger.info(s"On going active intimation = ${e.activeIntimationOpt.get} is ended at release date for employee = ${e.id} and leaves = $newLeaves are updated accordingly.")

                  List(
                    IntimationUpdated(e.id, reason, newRequests, now),
                    EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
                  )

                } else if (firstRequestDate.isAfter(today) && latestRequestDate.isAfter(today)) {
                  val newRequests = Set.empty[Request]
                  val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

                  newState = e.copy(leaves = newLeaves, activeIntimationOpt = None)

                  logger.info(s"Planned intimation = ${e.activeIntimationOpt.get} is cancelled for employee = ${e.id} and leaves = $newLeaves are updated accordingly.")

                  List(
                    IntimationCancelled(e.id, reason, newRequests, LocalDateTime.now()),
                    EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
                  )
                } else Nil
              } else Nil) ++
                (if (e.privilegedIntimationOpt.isDefined) {
                  val privilegedIntimation = e.privilegedIntimationOpt.get

                  val now = LocalDateTime.now()
                  val startDate = privilegedIntimation.start
                  val endDate = today

                  if ((privilegedIntimation.start.isBefore(today) || privilegedIntimation.start.isEqual(today)) && privilegedIntimation.end.isAfter(today)) {

                    val newRequests = between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

                    privilegedIntimation.privilegedIntimationType match {
                      case Maternity =>
                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)))
                        logger.info(s"On going privileged intimation = ${e.privilegedIntimationOpt.get} is ended at release date for employee = ${e.id}.")

                        List(PrivilegedIntimationUpdated(e.id, Maternity, startDate, endDate, s"$Maternity Leave", newRequests, now))

                      case Paternity =>
                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)))
                        logger.info(s"On going privileged intimation = ${e.privilegedIntimationOpt.get} is ended at release date for employee = ${e.id}.")

                        List(PrivilegedIntimationUpdated(e.id, Paternity, startDate, endDate, s"$Paternity Leave", newRequests, now))

                      case Sabbatical =>
                        val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)), leaves = newLeaves)
                        logger.info(s"On going privileged intimation = ${e.privilegedIntimationOpt.get} is ended at release date for employee = ${e.id} and leaves = $newLeaves are updated accordingly.")

                        List(
                          PrivilegedIntimationUpdated(e.id, Sabbatical, startDate, endDate, s"$Sabbatical Leave", newRequests, now),
                          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
                        )
                    }
                  } else if (privilegedIntimation.start.isAfter(today) && privilegedIntimation.start.isAfter(today)) {

                    val newRequests = Set.empty[Request]

                    privilegedIntimation.privilegedIntimationType match {
                      case Maternity =>
                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)))
                        logger.info(s"Planned privileged intimation = ${e.activeIntimationOpt.get} is cancelled for employee = ${e.id}.")

                        List(PrivilegedIntimationCancelled(e.id, Maternity, startDate, endDate, s"$Maternity Leave", newRequests, now))

                      case Paternity =>
                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)))
                        logger.info(s"Planned privileged intimation = ${e.activeIntimationOpt.get} is cancelled for employee = ${e.id}.")

                        List(PrivilegedIntimationCancelled(e.id, Paternity, startDate, endDate, s"$Paternity Leave", newRequests, now))

                      case Sabbatical =>
                        val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

                        newState = e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, endDate)), leaves = newLeaves)
                        logger.info(s"Planned privileged intimation = ${e.activeIntimationOpt.get} is cancelled for employee = ${e.id} and leaves = $newLeaves are updated accordingly.")

                        List(
                          PrivilegedIntimationCancelled(e.id, Sabbatical, startDate, endDate, s"$Sabbatical Leave", newRequests, now),
                          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
                        )
                    }
                  } else Nil
                } else Nil)
            } else Nil

          val (earnedCredits, sickCredits) = computeCredits(newState)

          lazy val activeIntimation = newState.activeIntimationOpt.get
          lazy val activeIntimationRequests = activeIntimation.requests
          lazy val latestRequestDate = activeIntimationRequests.map(_.date).toList.sortWith(_.isBefore(_)).last

          lazy val privilegedIntimation = newState.privilegedIntimationOpt.get
          lazy val privilegedIntimationRequests = between(privilegedIntimation.start, privilegedIntimation.end).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

          val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(today) || already5(latestRequestDate)
          val hasActiveSabbaticalPrivilegedIntimation = newState.privilegedIntimationOpt.isDefined &&
            newState.privilegedIntimationOpt.get.privilegedIntimationType == PrivilegedIntimationType.Sabbatical &&
            (privilegedIntimationRequests.last.date.isAfter(today) || !already5(privilegedIntimationRequests.last.date))

          if (hasNoActiveIntimationAvailable && (newState.privilegedIntimationOpt.isEmpty || !hasActiveSabbaticalPrivilegedIntimation)) {
            val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
            val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

            ctx.thenPersistAll(
              eventsToPersist ++
                List(
                  LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
                  LeavesCredited(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
                  EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
                  EmployeeReleased(newState.id, today)
                ): _*
            )(() => ctx.reply(Done))
          } else {
            val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
            val newLeaves = if(hasActiveSabbaticalPrivilegedIntimation) getNewLeaves(privilegedIntimationRequests, balanced) else getNewLeaves(activeIntimationRequests, balanced)

            ctx.thenPersistAll(
              eventsToPersist ++
                List(
                  LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
                  LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
                  EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
                  EmployeeReleased(newState.id, today)
                ): _*
            )(() => ctx.reply(Done))
          }
        }

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

        lazy val today = LocalDate.now

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val latestRequestDate = activeIntimation.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        lazy val privilegedIntimation = e.privilegedIntimationOpt.get

        if (intimationReq.requests.isEmpty || intimationReq.reason.trim == "") {
          val msg = "Intimation can't be created without any requests or a reason"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (intimationReq.requests.exists(_.date.isBefore(today)) || intimationReq.requests.exists(r => already5(r.date))) {
          val msg = s"Intimation can't be created for dates in the past"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (intimationReq.requests.exists(r => isWeekend(r.date))) {
          val msg = s"Intimation can't be created for weekends"

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (e.privilegedIntimationOpt.isDefined &&
          (privilegedIntimation.end.isAfter(today) || (privilegedIntimation.end.isEqual(today) && !already5(privilegedIntimation.end)))) {
          val msg = s"There already is an existing privileged intimation. Cancel the same in order to create a new one."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (e.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(today) || already5(latestRequestDate)) {
          val newLeaves = getNewLeaves(intimationReq.requests, lastLeaves = Leaves(e.leaves.earned, e.leaves.currentYearEarned, e.leaves.sick, e.leaves.extra))

          ctx.thenPersistAll(
            IntimationCreated(empId, intimationReq.reason, intimationReq.requests, LocalDateTime.now()),
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

        } else if (e.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
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
            IntimationUpdated(empId, intimationReq.reason, newRequests, LocalDateTime.now()),
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

        if (e.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(LocalDate.now()) || already5(latestRequestDate)) {
          val msg = s"No intimations found."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else {
          val requestsAlreadyConsumed = requests.filter(r => r.date.isBefore(LocalDate.now()) || already5(r.date))
          val newLeaves = getNewLeaves(requestsAlreadyConsumed, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

          ctx.thenPersistAll(
            IntimationCancelled(empId, reason, requestsAlreadyConsumed, LocalDateTime.now()),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(newLeaves))
        }

    }.onCommand[CreatePrivilegedIntimation, Leaves] {
      case (CreatePrivilegedIntimation(empId, privilegedIntimation), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreatePrivilegedIntimation command.")

        lazy val latestRequestDate = e.activeIntimationOpt.get.requests.map(_.date).toList.sortWith(_.isBefore(_)).last

        if (e.privilegedIntimationOpt.isDefined && e.privilegedIntimationOpt.get.end.isAfter(LocalDate.now())) {
          val msg = s"There already is an existing privileged intimation. Cancel the same in order to create a new one."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (e.activeIntimationOpt.isDefined && latestRequestDate.isAfter(LocalDate.now()) && !already5(latestRequestDate)) {
          val msg = s"Only single active intimation at a given time is supported. Cancel an active intimation first so as to create a new intimation."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (privilegedIntimation.end.isBefore(privilegedIntimation.start)) {
          val msg = s"Start date can't be after end date."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (privilegedIntimation.start.isBefore(LocalDate.now())) {
          val msg = s"Privileged intimations can't be created for the dates in the past."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else {
          val now = LocalDateTime.now()

          val startDate = privilegedIntimation.start
          val endDate = privilegedIntimation.end

          val requests = between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

          privilegedIntimation.privilegedIntimationType match {
            case Maternity =>
              ctx.thenPersist(PrivilegedIntimationCreated(empId, Maternity, startDate, endDate, s"$Maternity Leave", requests, now))(_ => ctx.reply(e.leaves))

            case Paternity =>
              ctx.thenPersist(PrivilegedIntimationCreated(empId, Paternity, startDate, endDate, s"$Paternity Leave", requests, now))(_ => ctx.reply(e.leaves))

            case Sabbatical =>
              val newLeaves = getNewLeaves(requests, lastLeaves = Leaves(e.leaves.earned, e.leaves.currentYearEarned, e.leaves.sick, e.leaves.extra))

              ctx.thenPersistAll(
                PrivilegedIntimationCreated(empId, Sabbatical, startDate, endDate, s"$Sabbatical Leave", requests, now),
                LastLeavesSaved(empId, e.leaves.earned, e.leaves.currentYearEarned, e.leaves.sick, e.leaves.extra),
                EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
              )(() => ctx.reply(newLeaves))
          }
        }

    }.onCommand[UpdatePrivilegedIntimation, Leaves] {
      case (UpdatePrivilegedIntimation(empId, privilegedIntimation), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdatePrivilegedIntimation command.")

        if (e.privilegedIntimationOpt.isEmpty || e.privilegedIntimationOpt.get.end.isBefore(LocalDate.now())) {
          val msg = s"No privileged intimation found to update."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (privilegedIntimation.end.isBefore(privilegedIntimation.start)) {
          val msg = s"Start date can't be after end date."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (privilegedIntimation.privilegedIntimationType != e.privilegedIntimationOpt.get.privilegedIntimationType) {
          val msg = s"Privileged intimation type is not allowed to be changed."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else if (privilegedIntimation.start.isBefore(LocalDate.now()) || already5(privilegedIntimation.start)) {
          val msg = s"Start date can't be updated since it is already in the past."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else {
          val now = LocalDateTime.now()

          val startDate = privilegedIntimation.start
          val endDate = privilegedIntimation.end

          val newRequests = between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

          privilegedIntimation.privilegedIntimationType match {
            case Maternity =>
              ctx.thenPersist(PrivilegedIntimationUpdated(empId, Maternity, startDate, endDate, s"$Maternity Leave", newRequests, now))(_ => ctx.reply(e.leaves))

            case Paternity =>
              ctx.thenPersist(PrivilegedIntimationUpdated(empId, Paternity, startDate, endDate, s"$Paternity Leave", newRequests, now))(_ => ctx.reply(e.leaves))

            case Sabbatical =>
              val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

              ctx.thenPersistAll(
                PrivilegedIntimationUpdated(empId, Sabbatical, startDate, endDate, s"$Sabbatical Leave", newRequests, now),
                EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
              )(() => ctx.reply(newLeaves))
          }
        }

    }.onCommand[CancelPrivilegedIntimation, Leaves] {
      case (CancelPrivilegedIntimation(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelPrivilegedIntimation command.")

        if (e.privilegedIntimationOpt.isEmpty || e.privilegedIntimationOpt.get.end.isBefore(LocalDate.now())) {
          val msg = s"No privileged intimation found to cancel."

          ctx.invalidCommand(msg)
          logger.error(s"InvalidCommandException: $msg")

          ctx.done

        } else {
          val now = LocalDateTime.now()

          val startDate = e.privilegedIntimationOpt.get.start
          val endDate = LocalDate.now()

          val requestsAlreadyConsumed = between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

          e.privilegedIntimationOpt.get.privilegedIntimationType match {
            case Maternity =>
              ctx.thenPersist(PrivilegedIntimationCancelled(empId, Maternity, startDate, endDate, s"$Maternity Leave", requestsAlreadyConsumed, now))(_ => ctx.reply(e.leaves))

            case Paternity =>
              ctx.thenPersist(PrivilegedIntimationUpdated(empId, Paternity, startDate, endDate, s"$Paternity Leave", requestsAlreadyConsumed, now))(_ => ctx.reply(e.leaves))

            case Sabbatical =>
              val newLeaves = getNewLeaves(requestsAlreadyConsumed, lastLeaves = Leaves(e.lastLeaves.earned, e.lastLeaves.currentYearEarned, e.lastLeaves.sick, e.lastLeaves.extra))

              ctx.thenPersistAll(
                PrivilegedIntimationCancelled(empId, Sabbatical, startDate, endDate, s"$Sabbatical Leave", requestsAlreadyConsumed, now),
                EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
              )(() => ctx.reply(newLeaves))
          }
        }

    }.onCommand[CreditLeaves, Done] {
      case (CreditLeaves(empId), ctx, state@Some(e)) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received ${CreditLeaves(empId)} command.")

        val today = LocalDate.now()

        val (earnedCredits, sickCredits) = computeCredits(e)

        lazy val activeIntimation = e.activeIntimationOpt.get
        lazy val activeIntimationRequests = activeIntimation.requests
        lazy val latestRequestDate = activeIntimationRequests.map(_.date).toList.sortWith(_.isBefore(_)).last

        lazy val privilegedIntimation = e.privilegedIntimationOpt.get
        lazy val privilegedIntimationRequests = between(privilegedIntimation.start, privilegedIntimation.end).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

        val hasNoActiveIntimationAvailable = e.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(today) || already5(latestRequestDate)
        val hasActiveSabbaticalPrivilegedIntimation = e.privilegedIntimationOpt.isDefined &&
          e.privilegedIntimationOpt.get.privilegedIntimationType == PrivilegedIntimationType.Sabbatical &&
          (privilegedIntimationRequests.last.date.isAfter(today) || !already5(privilegedIntimationRequests.last.date))

        if (hasNoActiveIntimationAvailable && (e.privilegedIntimationOpt.isEmpty || !hasActiveSabbaticalPrivilegedIntimation)) {
          val balanced = balanceExtra(e.leaves.earned + earnedCredits, e.leaves.currentYearEarned + earnedCredits, e.leaves.sick + sickCredits, e.leaves.extra)
          val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

          ctx.thenPersistAll(
            LastLeavesSaved(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            LeavesCredited(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
          )(() => ctx.reply(Done))
        } else {
          val balanced = balanceExtra(e.lastLeaves.earned + earnedCredits, e.lastLeaves.currentYearEarned + earnedCredits, e.lastLeaves.sick + sickCredits, e.lastLeaves.extra)
          val newLeaves = if(hasActiveSabbaticalPrivilegedIntimation) getNewLeaves(privilegedIntimationRequests, balanced) else getNewLeaves(activeIntimationRequests, balanced)

          ctx.thenPersistAll(
            LastLeavesSaved(e.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
            LeavesCredited(e.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
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
        Some(EmployeeState(id, name, gender, doj, dor, designation, pfn, contactInfo, location, leaves, roles, e.activeIntimationOpt, e.privilegedIntimationOpt, e.lastLeaves))

      case (EmployeeReleased(_, dor), Some(e)) =>
        Some(e.copy(dor = Some(dor)))

      case (EmployeeDeleted(_), _) =>
        None

      case (IntimationCreated(_, reason, requests, lastModified), Some(e)) =>
        Some(e.copy(activeIntimationOpt = Some(Intimation(reason, requests, lastModified))))

      case (IntimationUpdated(_, reason, requests, lastModified), Some(e)) =>
        Some(e.copy(activeIntimationOpt = Some(Intimation(reason, requests, lastModified))))

      case (IntimationCancelled(_, _, _, _), Some(e)) =>
        Some(e.copy(activeIntimationOpt = None))

      case (LastLeavesSaved(_, earned, currentYearEarned, sick, extra), Some(e)) =>
        Some(e.copy(lastLeaves = Leaves(earned, currentYearEarned, sick, extra)))

      case (LeavesCredited(_, earned, currentYearEarned, sick, extra), Some(e)) =>
        Some(e.copy(leaves = Leaves(earned, currentYearEarned, sick, extra)))

      case (LeavesBalanced(_, earned, _), Some(e)) =>
        Some(e.copy(leaves = Leaves(earned)))

      case (PrivilegedIntimationCreated(_, privilegedType, start, end, _, _, _), Some(e)) =>
        Some(e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(privilegedType, start, end))))

      case (PrivilegedIntimationUpdated(_, privilegedType, start, end, _, _, _), Some(e)) =>
        Some(e.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(privilegedType, start, end))))

      case (PrivilegedIntimationCancelled(_, _, _, _, _, _, _), Some(e)) =>
        Some(e.copy(privilegedIntimationOpt = None))
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
      case (DeleteEmployee(id), ctx, state) =>
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

    }.onCommand[CreatePrivilegedIntimation, Leaves] {
      case (CreatePrivilegedIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CreatePrivilegedIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[UpdatePrivilegedIntimation, Leaves] {
      case (UpdatePrivilegedIntimation(empId, _), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received UpdatePrivilegedIntimation command.")

        val msg = s"Employee with id = $empId has already been released."

        ctx.invalidCommand(msg)
        logger.error(s"InvalidCommandException: $msg")

        ctx.done

    }.onCommand[CancelPrivilegedIntimation, Leaves] {
      case (CancelPrivilegedIntimation(empId), ctx, state) =>
        logger.info(s"EmployeePersistenceEntity at state = $state received CancelPrivilegedIntimation command.")

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

  def isWeekend(date: LocalDate): Boolean = date.getDayOfWeek.toString == "SATURDAY" || date.getDayOfWeek.toString == "SUNDAY"

  def between(fromDate: LocalDate, toDate: LocalDate): immutable.Seq[LocalDate] =
    fromDate.toEpochDay.until(toDate.plusDays(1).toEpochDay).map(LocalDate.ofEpochDay)

  def already5(date: LocalDate): Boolean = {
    def dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    LocalDateTime.now().isAfter(LocalDateTime.parse(date.toString + " 17:00", dtf))
  }

  def getNewLeaves(requests: Set[Request], lastLeaves: Leaves): Leaves = {
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

  def balanceExtra(earned: Double, currentYearEarned: Double, sick: Double, due: Double): Leaves = {
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

  def computeCredits(state: EmployeeState): (Double, Double) = {

    val today = LocalDate.now

    val doj = state.doj

    val j = if (doj.getMonthValue == today.getMonthValue && doj.getYear == today.getYear) doj.getDayOfMonth else 1
    val r = if (state.dor.isDefined) state.dor.get.getDayOfMonth else 31

    val (s, e) = if (state.privilegedIntimationOpt.isEmpty) (0, 0) else {
      val privilegedIntimationType = state.privilegedIntimationOpt.get.privilegedIntimationType
      val pStart = state.privilegedIntimationOpt.get.start
      val pEnd = state.privilegedIntimationOpt.get.end

      if (privilegedIntimationType.equals(PrivilegedIntimationType.Paternity) ||
        (today.getMonthValue < pStart.getMonthValue && today.getYear < pStart.getYear) ||
        (today.getMonthValue > pEnd.getMonthValue && today.getYear > pEnd.getYear)) (0, 0)
      else (
        if (pStart.getMonthValue == today.getMonthValue && pStart.getYear == today.getYear) pStart.getDayOfMonth else 1,
        if (pEnd.getMonthValue == today.getMonthValue && pEnd.getYear == today.getYear && pEnd.getDayOfMonth < r) pEnd.getDayOfMonth else r
      )
    }

    val prorata = if (e == 0 && s == 0) r - j + 1 else (r - j) - (e - s)

    val el = if (prorata >= 22) 1.5 else if (prorata >= 16) 1 else if (prorata >= 10) 0.5 else 0
    val sl = if (prorata >= 15) 0.5 else 0

    (el, sl)
  }

}
