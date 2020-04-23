package com.codingkapoor.employee.impl

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codingkapoor.employee.api.models.PrivilegedIntimationType.{Maternity, Paternity, Sabbatical}
import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, Intimation, IntimationReq, Leaves, Location, PrivilegedIntimation, PrivilegedIntimationType, Request, RequestType, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity.{already5, balanceExtra, between, computeCredits, getNewLeaves, isWeekend}
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, BalanceLeaves, CancelIntimation, CreateIntimation, CreatePrivilegedIntimation, CreditLeaves, DeleteEmployee, EmployeeAdded, EmployeeCommand, EmployeeDeleted, EmployeeEvent, EmployeeReleased, EmployeeState, EmployeeUpdated, IntimationCancelled, IntimationCreated, IntimationUpdated, LastLeavesSaved, LeavesCredited, PrivilegedIntimationCancelled, PrivilegedIntimationUpdated, ReleaseEmployee, UpdateEmployee, UpdateIntimation}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

class EmployeePersistenceEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues with TypeCheckedTripleEquals {

  private val system = ActorSystem("EmployeePersistenceEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(EmployeeSerializerRegistry))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  val empId = 128L
  val employee@e = Employee(empId, "John Doe", "M", LocalDate.parse("2018-01-16"), None, "System Engineer", "PFN001",
    ContactInfo("+91-9912345678", "mail@johndoe.com"), Location(), Leaves(), List(Role.Employee))
  val employeeInfo@ei = EmployeeInfo(Some("Sr. System Engineer"), Some(ContactInfo("+91-9912345679", "mail1@johndoe.com")), Some(Location("Chennai", "Tamil Nadu")), Some(List(Role.Employee)))
  val state: EmployeeState = EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, Leaves(), e.roles, None, None, Leaves())

  private def withDriver[T](block: PersistentEntityTestDriver[EmployeeCommand[_], EmployeeEvent, Option[EmployeeState]] => T): T = {
    val driver = new PersistentEntityTestDriver(system, new EmployeePersistenceEntity, empId.toString)

    try {
      block(driver)
    } finally {
      driver.getAllIssues shouldBe empty
    }
  }

  "Employee persistence entity" should {

    // Test cases for initial state of the persistent entity
    "add an employee that doesn't already exists against a given employee id" in withDriver { driver =>
      val outcome = driver.run(AddEmployee(employee))

      outcome.events should contain only EmployeeAdded(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles)
      outcome.state should ===(Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles, None, None, Leaves())))
      outcome.issues should be(Nil)
    }

    "invalidate updation of a non existent employee" in withDriver { driver =>
      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate release of a non existent employee" in withDriver { driver =>
      val dor = LocalDate.parse("2020-04-17")

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate deletion of a non existent employee" in withDriver { driver =>
      val outcome = driver.run(DeleteEmployee(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for a non existent employee" in withDriver { driver =>
      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate updation of an intimation for a non existent employee" in withDriver { driver =>
      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an intimation for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(CancelIntimation(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate monthly credit of leaves for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(CreditLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate yearly balancing of leaves for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(BalanceLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    // Test cases for when an employee is already added
    "invalidate release of an employee that has admin privilege" in withDriver { driver =>
      val e1 = employee.copy(roles = List(Role.Admin, Role.Employee))

      driver.run(AddEmployee(e1))

      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate adding an employee with an id against which an employee already exists" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(AddEmployee(employee))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "update an already existing employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      val designation = employeeInfo.designation.getOrElse(e.designation)
      val contactInfo = employeeInfo.contactInfo.getOrElse(e.contactInfo)
      val location = employeeInfo.location.getOrElse(e.location)
      val roles = employeeInfo.roles.getOrElse(e.roles)

      outcome.events should contain only EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles)
      outcome.state should ===(Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles, None, None, Leaves())))
      outcome.issues should be(Nil)
      outcome.replies should contain only Employee(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles)
    }

    "release and credit leaves for an employee that has no ongoing intimations" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val (earnedCredits, sickCredits) = computeCredits(state)
      val balanced = balanceExtra(state.leaves.earned + earnedCredits, state.leaves.currentYearEarned + earnedCredits, state.leaves.sick + sickCredits, state.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      outcome.events should ===(
        List(
          LastLeavesSaved(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles),
          EmployeeReleased(state.id, dor)
        )
      )
    }

    // Few dates already consumed would mean that active intimation would require to be updated instead of getting cancelled and
    // release date as one of requested dates would mean that active intimation update wouldn't render active intimation as inactive
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and has release date as one of the requested dates" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val requests =
        Set(
          Request(dor.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(dor.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor, RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = requests.filterNot(r => r.date.isAfter(dor))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val iu = outcome.events.toList.head.asInstanceOf[IntimationUpdated]
      val outcomeIntimationUpdated = IntimationUpdated(iu.empId, iu.reason, iu.requests, now)

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(dor) ||
        (if (dor.isEqual(today)) already5(latestRequestDate) else false)

      val (balanced, newLeaves2) = if (hasNoActiveIntimationAvailable) {
        val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
        (balanced, Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      } else {
        val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
        (balanced, getNewLeaves(newState.activeIntimationOpt.get.requests, balanced))
      }

      outcomeIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          IntimationUpdated(e.id, initialState.activeIntimationOpt.get.reason, newRequests, now),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    // Few dates already consumed would mean that active intimation would require to be updated instead of getting cancelled and
    // release date as not one of requested dates would mean that active intimation update would render active intimation as inactive
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and not having release date as one of the requested dates" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val requests =
        Set(
          Request(dor.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(dor.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = requests.filterNot(r => r.date.isAfter(dor))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves2 = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val iu = outcome.events.toList.head.asInstanceOf[IntimationUpdated]
      val outcomeIntimationUpdated = IntimationUpdated(iu.empId, iu.reason, iu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          IntimationUpdated(e.id, initialState.activeIntimationOpt.get.reason, newRequests, now),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    // Release date as latest requested dates means that no update/cancel would be required for active intimation
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and having release date as the latest requested dates" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val requests =
        Set(
          Request(dor.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(dor.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor, RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = requests.filterNot(r => r.date.isAfter(dor))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(dor) ||
        (if (dor.isEqual(today)) already5(latestRequestDate) else false)

      val (balanced, newLeaves2) = if (hasNoActiveIntimationAvailable) {
        val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
        (balanced, Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      } else {
        val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
        (balanced, getNewLeaves(newState.activeIntimationOpt.get.requests, balanced))
      }

      outcome.events should ===(
        List(
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing active intimation that has first request date as release date" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val requests =
        Set(
          Request(dor, RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = requests.filterNot(r => r.date.isAfter(dor))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(dor) ||
        (if (dor.isEqual(today)) already5(latestRequestDate) else false)

      val (balanced, newLeaves2) = if (hasNoActiveIntimationAvailable) {
        val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
        (balanced, Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      } else {
        val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
        (balanced, getNewLeaves(newState.activeIntimationOpt.get.requests, balanced))
      }

      val iu = outcome.events.toList.head.asInstanceOf[IntimationUpdated]
      val outcomeIntimationUpdated = IntimationUpdated(iu.empId, iu.reason, iu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          IntimationUpdated(e.id, initialState.activeIntimationOpt.get.reason, newRequests, now),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing active intimation that has all request dates in future" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val requests =
        Set(
          Request(dor.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(dor.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = requests.filterNot(r => r.date.isAfter(dor))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
      val newLeaves2 = getNewLeaves(newState.activeIntimationOpt.get.requests, balanced)

      val iu = outcome.events.toList.head.asInstanceOf[IntimationCancelled]
      val outcomeIntimationCancelled = IntimationCancelled(iu.empId, iu.reason, iu.requests, now)

      outcomeIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          IntimationCancelled(e.id, initialState.activeIntimationOpt.get.reason, newRequests, now),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged maternity intimation that has few dates that are already consumed" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.minusDays(2)
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = EmployeePersistenceEntity.between(startDate, dor).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Maternity, startDate, dor, s"$Maternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged maternity intimation that has first request date as release date" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set(Request(dor, RequestType.Leave, RequestType.Leave))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Maternity, startDate, dor, s"$Maternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged maternity intimation that has all request dates in future" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.plusDays(2)
      val endDate = dor.plusDays(8)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set.empty[Request]
      val newState = initialState.copy(privilegedIntimationOpt = None)

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)

      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(e.id, Maternity, startDate, dor, s"$Maternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has few dates that are already consumed" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.minusDays(2)
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val newRequests = EmployeePersistenceEntity.between(startDate, dor).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Paternity, startDate, dor, s"$Paternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has first reuqest date as release date" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set(Request(dor, RequestType.Leave, RequestType.Leave))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Paternity, startDate, dor, s"$Paternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has all request dates in future" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.plusDays(2)
      val endDate = dor.plusDays(8)

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set.empty[Request]
      val newState = initialState.copy(privilegedIntimationOpt = None)

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)

      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(e.id, Paternity, startDate, dor, s"$Paternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged sabbatical intimation that has few dates that are already consumed" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.minusDays(2)
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = EmployeePersistenceEntity.between(startDate, dor).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Sabbatical, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val hasActiveSabbaticalPrivilegedIntimation = newState.privilegedIntimationOpt.isDefined &&
        newState.privilegedIntimationOpt.get.privilegedIntimationType == PrivilegedIntimationType.Sabbatical &&
        newRequests.last.date.isEqual(dor) && (if (dor.isEqual(today)) !already5(newRequests.last.date) else true)

      val (balanced, newLeaves2) = if (!hasActiveSabbaticalPrivilegedIntimation) {
        val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
        (balanced, Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      } else {
        val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
        (balanced, getNewLeaves(newRequests, balanced))
      }

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Sabbatical, startDate, dor, s"$Sabbatical Leave", newRequests, now),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged sabbatical intimation that has first request date as release date" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor
      val endDate = dor.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set(Request(dor, RequestType.Leave, RequestType.Leave))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Sabbatical, startDate, dor)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val hasActiveSabbaticalPrivilegedIntimation = newState.privilegedIntimationOpt.isDefined &&
        newState.privilegedIntimationOpt.get.privilegedIntimationType == PrivilegedIntimationType.Sabbatical &&
        newRequests.last.date.isEqual(dor) && (if (dor.isEqual(today)) !already5(newRequests.last.date) else true)

      val (balanced, newLeaves2) = if (!hasActiveSabbaticalPrivilegedIntimation) {
        val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
        (balanced, Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      } else {
        val balanced = balanceExtra(newState.lastLeaves.earned + earnedCredits, newState.lastLeaves.currentYearEarned + earnedCredits, newState.lastLeaves.sick + sickCredits, newState.lastLeaves.extra)
        (balanced, getNewLeaves(newRequests, balanced))
      }

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Sabbatical, startDate, dor, s"$Sabbatical Leave", newRequests, now),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves2.earned, newLeaves2.currentYearEarned, newLeaves2.sick, newLeaves2.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged sabbatical intimation that has all request dates in future" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val startDate = dor.plusDays(2)
      val endDate = dor.plusDays(8)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      val newRequests = Set.empty[Request]
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val newState = initialState.copy(leaves = Leaves(), privilegedIntimationOpt = None)

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves2 = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val now = LocalDateTime.now()
      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)

      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(e.id, Sabbatical, startDate, dor, s"$Sabbatical Leave", newRequests, now),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves2, newState.roles),
          EmployeeReleased(newState.id, dor)
        )
      )
    }

    "invalidate deletion of an already existing employee that is an admin" in withDriver { driver =>
      val e = employee.copy(roles = List(Role.Employee, Role.Admin))
      driver.run(AddEmployee(e))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "delete an already existing non admin employee" ignore withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events should contain only EmployeeDeleted(empId)
      outcome.state should be(None)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when no reason or request provided" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))

      val intimationReq = IntimationReq("", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)

      val intimationReq2 = IntimationReq("Reason", Set())

      val outcome2 = driver.run(CreateIntimation(empId, intimationReq2))

      outcome2.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome2.events.size should ===(0)
      outcome2.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when a provided request date is on a weekend" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val today = LocalDate.now()
      val weekend = today.plusDays(6 - today.getDayOfWeek.getValue)

      val intimationReq = IntimationReq("Reason", Set(Request(weekend, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when a provided request date is in the past" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val today = LocalDate.now()

      val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(today.minusDays(1), RequestType.Leave, RequestType.Leave)))))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)

      if (already5(today)) {
        val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(today, RequestType.Leave, RequestType.Leave)))))

        outcome.replies.head.getClass should be(classOf[InvalidCommandException])
        outcome.events.size should ===(0)
        outcome.issues should be(Nil)
      }
    }

    "invalidate creation of an intimation for an already existing employee when there is an already existing privileged intimation" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))
      driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(PrivilegedIntimationType.Maternity, requestDate, requestDate.plusDays(3))))

      val intimationReq = IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when there is an already existing intimation" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))

      driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))))
      val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "create intimation for an already existing employee when active intimation in the employee state is none" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))

      val intimationReq = IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      val newLeaves = getNewLeaves(intimationReq.requests, lastLeaves = Leaves(state.leaves.earned, state.leaves.currentYearEarned, state.leaves.sick, state.leaves.extra))

      val now = LocalDateTime.now()
      val ic = outcome.events.toList.head.asInstanceOf[IntimationCreated]
      val outcomeIntimationCreated = IntimationCreated(ic.empId, ic.reason, ic.requests, now)

      outcomeIntimationCreated :: outcome.events.toList.tail should ===(
        List(
          IntimationCreated(empId, intimationReq.reason, intimationReq.requests, now),
          LastLeavesSaved(empId, state.leaves.earned, state.leaves.currentYearEarned, state.leaves.sick, state.leaves.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles)
        )
      )
      outcome.replies should contain only newLeaves

      val es = outcome.state.get
      val ai = es.activeIntimationOpt.get

      val outcomeEmployeeState = es.copy(activeIntimationOpt = Some(Intimation(ai.reason, ai.requests, now)))
      val newState = state.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(intimationReq.reason, intimationReq.requests, now)), lastLeaves = state.leaves)

      outcomeEmployeeState should ===(newState)

      outcome.issues should be(Nil)
    }

    "create intimation for an already existing employee when active intimation in the employee state is not none but instead it's inactive" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val requests = if(already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(tomorrow.minusDays(3), RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      val newLeaves = getNewLeaves(intimationReq.requests, lastLeaves = Leaves(initialState.leaves.earned, initialState.leaves.currentYearEarned, initialState.leaves.sick, initialState.leaves.extra))

      val now = LocalDateTime.now()
      val ic = outcome.events.toList.head.asInstanceOf[IntimationCreated]
      val outcomeIntimationCreated = IntimationCreated(ic.empId, ic.reason, ic.requests, now)

      outcomeIntimationCreated :: outcome.events.toList.tail should ===(
        List(
          IntimationCreated(empId, intimationReq.reason, intimationReq.requests, now),
          LastLeavesSaved(empId, initialState.leaves.earned, initialState.leaves.currentYearEarned, initialState.leaves.sick, initialState.leaves.extra),
          EmployeeUpdated(initialState.id, initialState.name, initialState.gender, initialState.doj, initialState.dor, initialState.designation, initialState.pfn, initialState.contactInfo, initialState.location, newLeaves, initialState.roles)
        )
      )
      outcome.replies should contain only newLeaves

      val es = outcome.state.get
      val ai = es.activeIntimationOpt.get

      val outcomeEmployeeState = es.copy(activeIntimationOpt = Some(Intimation(ai.reason, ai.requests, now)))
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(intimationReq.reason, intimationReq.requests, now)), lastLeaves = initialState.leaves)

      outcomeEmployeeState should ===(newState)

      outcome.issues should be(Nil)
    }

    // Test cases for when an employee has already been released
    "invalidate adding an employee that already exists but has been released" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(AddEmployee(employee))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate updation of an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate release of an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate deletion of an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events should contain only EmployeeDeleted(e.id)
      outcome.state should ===(None)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate updation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(CancelIntimation(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate monthly credit of leaves for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(CreditLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate yearly balancing of leaves for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId, dor))

      val outcome = driver.run(BalanceLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }
  }
}
