package com.codingkapoor.employee.impl

import java.time.{LocalDate, LocalDateTime}

import akka.Done
import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codingkapoor.employee.api.models.PrivilegedIntimationType.{Maternity, Paternity, Sabbatical}
import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, Intimation, IntimationReq, Leaves, Location, PrivilegedIntimation, PrivilegedIntimationType, Request, RequestType, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity.{already5, balanceExtra, between, computeCredits, getNewLeaves, isWeekend}
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, BalanceLeaves, CancelIntimation, CancelPrivilegedIntimation, CreateIntimation, CreatePrivilegedIntimation, CreditLeaves, DeleteEmployee, EmployeeAdded, EmployeeCommand, EmployeeDeleted, EmployeeEvent, EmployeeReleased, EmployeeState, EmployeeUpdated, IntimationCancelled, IntimationCreated, IntimationUpdated, LastLeavesSaved, LeavesCredited, PrivilegedIntimationCancelled, PrivilegedIntimationCreated, PrivilegedIntimationUpdated, ReleaseEmployee, UpdateEmployee, UpdateIntimation, UpdatePrivilegedIntimation}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

class EmployeePersistenceEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues with TypeCheckedTripleEquals {

  private val system = ActorSystem("EmployeePersistenceEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(EmployeeSerializerRegistry))

  override def afterAll: Unit = {
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
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "invalidate updation of a non existent employee" in withDriver { driver =>
      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.events should be(Nil)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate release of a non existent employee" in withDriver { driver =>
      val dor = LocalDate.parse("2020-04-17")

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate deletion of a non existent employee" in withDriver { driver =>
      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for a non existent employee" in withDriver { driver =>
      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of an intimation for a non existent employee" in withDriver { driver =>
      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an intimation for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(CancelIntimation(empId))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate monthly credit of leaves for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(CreditLeaves(empId))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate yearly balancing of leaves for a non existent employee" in withDriver { driver =>
      val outcome = driver.run(BalanceLeaves(empId))

      outcome.events.size should ===(0)
      outcome.state should be(None)
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    // Test cases for when an employee is already added
    "invalidate release of an employee that has admin privilege" in withDriver { driver =>
      val e1 = employee.copy(roles = List(Role.Admin, Role.Employee))

      driver.run(AddEmployee(e1))

      val today = LocalDate.now()

      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state.copy(roles = e1.roles)))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate adding an employee with an id against which an employee already exists" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(AddEmployee(employee))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "ignore update of an already existing employee when employeeInfo has no changes" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val designation = e.designation
      val contactInfo = e.contactInfo
      val ei = employeeInfo.copy(designation = Some(designation), contactInfo = Some(contactInfo), location = None, roles = None)

      val outcome = driver.run(UpdateEmployee(empId, ei))

      outcome.events should be(Nil)
      outcome.state should ===(Some(state))
      outcome.replies should be(Nil)
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
      outcome.replies should contain only Employee(e.id, e.name, e.gender, e.doj, e.dor, designation, e.pfn, contactInfo, location, e.leaves, roles)
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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

      val outcomeActiveIntimation = outcome.state.get.activeIntimationOpt.get
      outcome.state.get.copy(activeIntimationOpt = Some(outcomeActiveIntimation.copy(lastModified = now))) should be(
        state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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

      val outcomeActiveIntimation = outcome.state.get.activeIntimationOpt.get
      outcome.state.get.copy(activeIntimationOpt = Some(outcomeActiveIntimation.copy(lastModified = now))) should be(
        state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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

      val outcomeActiveIntimation = outcome.state.get.activeIntimationOpt.get
      outcome.state.get.copy(activeIntimationOpt = Some(outcomeActiveIntimation.copy(lastModified = now))) should be(
        state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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

      val outcomeActiveIntimation = outcome.state.get.activeIntimationOpt.get
      outcome.state.get.copy(activeIntimationOpt = Some(outcomeActiveIntimation.copy(lastModified = now))) should be(
        state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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

      val finalState = state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))
      outcome.state should be(Some(finalState))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "release and credit leaves for an employee that has an ongoing privileged maternity intimation that has first request date as release date" in withDriver { driver =>
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
        privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
        privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has first request date as release date" in withDriver { driver =>
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
        privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
        privilegedIntimationOpt = Some(PrivilegedIntimation(Sabbatical, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
        privilegedIntimationOpt = Some(PrivilegedIntimation(Sabbatical, startDate, dor)))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
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
      outcome.state should be(Some(state.copy(dor = Some(dor), leaves = newLeaves2, lastLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra))))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "invalidate deletion of an already existing employee that is an admin" in withDriver { driver =>
      val initialState = state.copy(roles = List(Role.Employee, Role.Admin))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "delete an already existing non admin employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events should contain only EmployeeDeleted(empId)
      outcome.state should be(None)
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when no reason or request provided" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))

      val intimationReq = IntimationReq("", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)

      val intimationReq2 = IntimationReq("Reason", Set())

      val outcome2 = driver.run(CreateIntimation(empId, intimationReq2))

      outcome2.events.size should ===(0)
      outcome2.state should be(Some(state))
      outcome2.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome2.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when a provided request date is on a weekend" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val today = LocalDate.now()
      val weekend = today.plusDays(6 - today.getDayOfWeek.getValue)

      val intimationReq = IntimationReq("Reason", Set(Request(weekend, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when a provided request date is in the past" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val today = LocalDate.now()

      val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(today.minusDays(1), RequestType.Leave, RequestType.Leave)))))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)

      if (already5(today)) {
        val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(today, RequestType.Leave, RequestType.Leave)))))

        outcome.events.size should ===(0)
        outcome.state should be(Some(state))
        outcome.replies.head.getClass should be(classOf[InvalidCommandException])
        outcome.issues should be(Nil)
      }
    }

    "invalidate creation of an intimation for an already existing employee when there is an already existing privileged intimation" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val initialState = state.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(PrivilegedIntimationType.Maternity, requestDate, requestDate.plusDays(3))))

      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already existing employee when there is an already existing intimation" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val now = LocalDateTime.now()
      val initialState = state.copy(activeIntimationOpt = Some(Intimation(reason = "Reason", requests = Set(Request(requestDate, RequestType.Leave, RequestType.Leave)), lastModified = now)))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreateIntimation(empId, IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
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

      val requests = if (already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(tomorrow.minusDays(3), RequestType.Leave, RequestType.Leave))
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

    "invalidate updation of an intimation for an already existing employee when no reason or request provided" in withDriver { driver =>
      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      driver.run(AddEmployee(employee))

      val outcome = driver.run(UpdateIntimation(empId, IntimationReq("", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)

      val outcome2 = driver.run(UpdateIntimation(empId, IntimationReq("Reason", Set())))

      outcome2.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome2.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome2.issues should be(Nil)
    }

    "invalidate updation of a non existent active intimation of an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val tomorrow = LocalDate.now().plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val intimationReq = IntimationReq("", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of request dates from the past in an active intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val requestDate1 = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val requestDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val requests = Set(
        Request(requestDate1, RequestType.Leave, RequestType.Leave),
        Request(requestDate2, RequestType.Leave, RequestType.Leave)
      )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("", Set(Request(requestDate1.minusDays(1), RequestType.Leave, RequestType.Leave), Request(requestDate1, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of an active intimation for request dates on weekends for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val requests = Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val weekend = today.plusDays(6 - today.getDayOfWeek.getValue)
      val intimationReq = IntimationReq("Reason", Set(Request(weekend, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "ignore request to update active intimation for an already existing employee when the request dates are similar to the same in the ongoing active intimation" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val requests = Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("Reason", Set(Request(requestDate, RequestType.Leave, RequestType.Leave)))

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.replies should be(Nil)
      outcome.events should be(Nil)
      outcome.state should ===(Some(initialState))
      outcome.issues should be(Nil)
    }

    "update active intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val dayAfterTomorrow = tomorrow.plusDays(1)
      val requestDate1 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val requestDate2 = if (isWeekend(dayAfterTomorrow)) dayAfterTomorrow.plusDays(2) else dayAfterTomorrow

      val requests = Set(Request(requestDate1, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val newRequests = Set(Request(requestDate2, RequestType.Leave, RequestType.Leave))
      val intimationReq = IntimationReq("Reason", newRequests)

      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val iu = outcome.events.toList.head.asInstanceOf[IntimationUpdated]
      val outcomeIntimationUpdated = IntimationUpdated(iu.empId, iu.reason, iu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          IntimationUpdated(empId, intimationReq.reason, newRequests, now),
          EmployeeUpdated(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, newLeaves, e.roles)
        )
      )

      val oai = outcome.state.get.activeIntimationOpt.get
      outcome.state.get.copy(activeIntimationOpt = Some(Intimation(oai.reason, oai.requests, now))) should ===(
        initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(intimationReq.reason, newRequests, now))))
      outcome.replies should contain only newLeaves
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of a non existent active intimation of an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val outcome = driver.run(CancelIntimation(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of inactive intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val requestDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday

      val requests = if (already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelIntimation(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of a privileged intimation for an already existing employee when a privileged intimation already exists" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)

      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate, endDate)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of a privileged intimation for an already existing employee when an active intimation already exists" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val requestDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val requests = if (!already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate, endDate)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of a privileged intimation for an already existing employee for which start and end dates are not in proper order" in withDriver { driver =>
      driver.initialize((Some(Some(state))))

      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, endDate, startDate)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of a privileged intimation in the past for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val endDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val outcome = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate, endDate)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate creation of a privileged intimation for start or end dates on weekends for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val startDate = today.plusDays(6 - today.getDayOfWeek.getValue)
      val endDate = startDate.plusDays(2)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate, endDate)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)

      val endDate2 = today.plusDays(6 - today.getDayOfWeek.getValue)
      val startDate2 = endDate2.minusDays(2)

      val outcome2 = driver.run(CreatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate2, endDate2)))

      outcome2.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome2.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome2.issues should be(Nil)
    }

    "create maternity privileged intimation for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val maternityPrivilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, maternityPrivilegedIntimation))

      val requests = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val mpic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCreated]
      val outcomeIntimationCreated = PrivilegedIntimationCreated(mpic.empId, mpic.privilegedIntimationType, mpic.start, mpic.end, mpic.reason, mpic.requests, now)

      outcomeIntimationCreated :: outcome.events.toList.tail should contain only PrivilegedIntimationCreated(empId, Maternity, startDate, endDate, s"$Maternity Leave", requests, now)
      outcome.state should ===(Some(state.copy(privilegedIntimationOpt = Some(maternityPrivilegedIntimation))))
      outcome.replies should contain only state.leaves
      outcome.issues should be(Nil)
    }

    "create paternity privileged intimation for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val paternityPrivilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, paternityPrivilegedIntimation))

      val requests = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val ppic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCreated]
      val outcomeIntimationCreated = PrivilegedIntimationCreated(ppic.empId, ppic.privilegedIntimationType, ppic.start, ppic.end, ppic.reason, ppic.requests, now)

      outcomeIntimationCreated :: outcome.events.toList.tail should contain only PrivilegedIntimationCreated(empId, Paternity, startDate, endDate, s"$Paternity Leave", requests, now)
      outcome.state should ===(Some(state.copy(privilegedIntimationOpt = Some(paternityPrivilegedIntimation))))
      outcome.replies should contain only state.leaves
      outcome.issues should be(Nil)
    }

    "create sabbatical privileged intimation for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val sabbaticalPrivilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)

      val outcome = driver.run(CreatePrivilegedIntimation(empId, sabbaticalPrivilegedIntimation))

      val requests = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val spic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCreated]
      val outcomeIntimationCreated = PrivilegedIntimationCreated(spic.empId, spic.privilegedIntimationType, spic.start, spic.end, spic.reason, spic.requests, now)

      val newLeaves = getNewLeaves(requests, lastLeaves = Leaves(state.leaves.earned, state.leaves.currentYearEarned, state.leaves.sick, state.leaves.extra))

      outcomeIntimationCreated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCreated(empId, Sabbatical, startDate, endDate, s"$Sabbatical Leave", requests, now),
          LastLeavesSaved(empId, state.leaves.earned, state.leaves.currentYearEarned, state.leaves.sick, state.leaves.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles)
        )
      )
      outcome.state should ===(Some(state.copy(leaves = newLeaves, privilegedIntimationOpt = Some(sabbaticalPrivilegedIntimation))))
      outcome.replies should contain only newLeaves
      outcome.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee if found none" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, privilegedIntimation))

      outcome.events.size should ===(0)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of an inactive privileged intimation for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val endDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val startDate = if (isWeekend(yesterday.minusDays(4))) yesterday.minusDays(6) else yesterday.minusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))
      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation2 = PrivilegedIntimation(Maternity, startDate2, endDate2)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, privilegedIntimation2))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee when start and end dates are not in proper order" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val privilegedIntimation2 = PrivilegedIntimation(Maternity, endDate, startDate)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, privilegedIntimation2))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee when an attempt is made to modify the privileged intimation type" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(3) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation2 = PrivilegedIntimation(Paternity, startDate2, endDate2)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, privilegedIntimation2))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee when provided start or end dates are on weekends" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = today.plusDays(6 - today.getDayOfWeek.getValue)
      val endDate2 = startDate2.plusDays(2)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate2, endDate2)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)

      val endDate3 = today.plusDays(6 - today.getDayOfWeek.getValue)
      val startDate3 = endDate3.minusDays(2)

      val outcome2 = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate3, endDate3)))

      outcome2.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome2.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome2.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee when provided start date is in the past" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (already5(today)) today else {
        if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      }
      val endDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate2, endDate2)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of a privileged intimation for an already existing employee when attempt is made to modify start date that's already in the past" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val endDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate2, endDate2)))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "ignore request to update privileged intimation for an already existing employee when start and end dates in the request is same as the ongoing privileged intimation" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val privilegedIntimation2 = PrivilegedIntimation(Paternity, startDate, endDate)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, privilegedIntimation2))

      outcome.events should be(Nil)
      outcome.state should ===(Some(initialState))
      outcome.replies should be(Nil)
      outcome.issues should be(Nil)
    }

    "update maternity privileged intimation for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Maternity, startDate2, endDate2)))

      val now = LocalDateTime.now()
      val newRequests = EmployeePersistenceEntity.between(startDate2, endDate2).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val mpiu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomeIntimationUpdated = PrivilegedIntimationUpdated(mpiu.empId, mpiu.privilegedIntimationType, mpiu.start, mpiu.end, mpiu.reason, mpiu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should contain only PrivilegedIntimationUpdated(empId, Maternity, startDate2, endDate2, s"$Maternity Leave", newRequests, now)
      outcome.state should ===(Some(initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate2, endDate2)))))
      outcome.replies should contain only Leaves()
      outcome.issues should be(Nil)
    }

    "update paternity privileged intimation for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Paternity, startDate2, endDate2)))

      val now = LocalDateTime.now()
      val newRequests = EmployeePersistenceEntity.between(startDate2, endDate2).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val mpiu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomeIntimationUpdated = PrivilegedIntimationUpdated(mpiu.empId, mpiu.privilegedIntimationType, mpiu.start, mpiu.end, mpiu.reason, mpiu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should contain only PrivilegedIntimationUpdated(empId, Paternity, startDate2, endDate2, s"$Paternity Leave", newRequests, now)
      outcome.state should ===(Some(initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate2, endDate2)))))
      outcome.replies should contain only Leaves()
      outcome.issues should be(Nil)
    }

    "update sabbatical privileged intimation for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val startDate2 = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate2 = if (isWeekend(tomorrow.plusDays(4))) tomorrow.plusDays(6) else tomorrow.plusDays(4)

      val outcome = driver.run(UpdatePrivilegedIntimation(empId, PrivilegedIntimation(Sabbatical, startDate2, endDate2)))

      val now = LocalDateTime.now()
      val newRequests = EmployeePersistenceEntity.between(startDate2, endDate2).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val mpiu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomeIntimationUpdated = PrivilegedIntimationUpdated(mpiu.empId, mpiu.privilegedIntimationType, mpiu.start, mpiu.end, mpiu.reason, mpiu.requests, now)

      outcomeIntimationUpdated :: outcome.events.toList.tail should ===(List(
        PrivilegedIntimationUpdated(empId, Sabbatical, startDate2, endDate2, s"$Sabbatical Leave", newRequests, now),
        EmployeeUpdated(initialState.id, initialState.name, initialState.gender, initialState.doj, initialState.dor, initialState.designation, initialState.pfn, initialState.contactInfo, initialState.location, newLeaves, initialState.roles)
      ))
      outcome.state should ===(Some(initialState.copy(leaves = newLeaves, privilegedIntimationOpt = Some(PrivilegedIntimation(Sabbatical, startDate2, endDate2)))))
      outcome.replies should contain only newLeaves
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of a non existent privileged intimation for an already existing employee" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val outcome = driver.run(CancelPrivilegedIntimation(empId))

      outcome.events should be(Nil)
      outcome.state should be(Some(state))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an inactive privileged intimation for an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val endDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val startDate = if (isWeekend(yesterday.minusDays(4))) yesterday.minusDays(6) else yesterday.minusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelPrivilegedIntimation(empId))

      outcome.events should be(Nil)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "cancel maternity privileged intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelPrivilegedIntimation(empId))

      val now = LocalDateTime.now()
      val requestsAlreadyConsumed = EmployeePersistenceEntity.between(initialState.privilegedIntimationOpt.get.start, today).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)
      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(List(PrivilegedIntimationCancelled(empId, Maternity, startDate, today, s"$Maternity Leave", requestsAlreadyConsumed, now)))

      outcome.state should be(Some(state))
      outcome.replies should contain only initialState.leaves
      outcome.issues should be(Nil)
    }

    "cancel paternity privileged intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelPrivilegedIntimation(empId))

      val now = LocalDateTime.now()
      val requestsAlreadyConsumed = EmployeePersistenceEntity.between(initialState.privilegedIntimationOpt.get.start, today).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)
      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(List(PrivilegedIntimationCancelled(empId, Paternity, startDate, today, s"$Paternity Leave", requestsAlreadyConsumed, now)))

      outcome.state should be(Some(state))
      outcome.replies should contain only initialState.leaves
      outcome.issues should be(Nil)
    }

    "cancel sabbatical privileged intimation of an already existing employee" in withDriver { driver =>
      val today = LocalDate.now()
      val tomorrow = today.plusDays(1)
      val startDate = if (isWeekend(tomorrow)) tomorrow.plusDays(2) else tomorrow
      val endDate = if (isWeekend(tomorrow.plusDays(3))) tomorrow.plusDays(5) else tomorrow.plusDays(3)

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelPrivilegedIntimation(empId))

      val now = LocalDateTime.now()
      val requestsAlreadyConsumed = EmployeePersistenceEntity.between(initialState.privilegedIntimationOpt.get.start, today).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet
      val newLeaves = getNewLeaves(requestsAlreadyConsumed, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)
      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(empId, Sabbatical, startDate, today, s"$Sabbatical Leave", requestsAlreadyConsumed, now),
          EmployeeUpdated(initialState.id, initialState.name, initialState.gender, initialState.doj, initialState.dor, initialState.designation, initialState.pfn, initialState.contactInfo, initialState.location, newLeaves, initialState.roles)
        )
      )

      outcome.state should be(Some(state))
      outcome.replies should contain only initialState.leaves
      outcome.issues should be(Nil)
    }

    "credit leaves when there is a non-exitent active and a non-existent privileged intimations" in withDriver { driver =>
      driver.initialize(Some(Some(state)))

      val outcome = driver.run(CreditLeaves(empId))

      val (earnedCredits, sickCredits) = computeCredits(state)
      val balanced = balanceExtra(state.leaves.earned + earnedCredits, state.leaves.currentYearEarned + earnedCredits, state.leaves.sick + sickCredits, state.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      outcome.events should ===(
        List(
          LastLeavesSaved(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles)
        )
      )
      outcome.state should be(Some(state.copy(leaves = newLeaves, lastLeaves = newLeaves)))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "credit leaves when there is an inactive and a non-existent privileged intimation" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val requestDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday

      val requests = if (already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val is@initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreditLeaves(empId))

      val (earnedCredits, sickCredits) = computeCredits(initialState)
      val balanced = balanceExtra(is.leaves.earned + earnedCredits, is.leaves.currentYearEarned + earnedCredits, is.leaves.sick + sickCredits, is.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      outcome.events should ===(
        List(
          LastLeavesSaved(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(is.id, is.name, is.gender, is.doj, is.dor, is.designation, is.pfn, is.contactInfo, is.location, newLeaves, is.roles)
        )
      )
      outcome.state should be(Some(initialState.copy(leaves = newLeaves, lastLeaves = newLeaves)))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "credit leaves when there is a non-exitent active intimation and an inactive sabbatical privileged intimation" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val endDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val startDate = if (isWeekend(yesterday.minusDays(4))) yesterday.minusDays(6) else yesterday.minusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
      val is@initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreditLeaves(empId))

      val (earnedCredits, sickCredits) = computeCredits(initialState)
      val balanced = balanceExtra(is.leaves.earned + earnedCredits, is.leaves.currentYearEarned + earnedCredits, is.leaves.sick + sickCredits, is.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      outcome.events should ===(
        List(
          LastLeavesSaved(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(is.id, is.name, is.gender, is.doj, is.dor, is.designation, is.pfn, is.contactInfo, is.location, newLeaves, is.roles)
        )
      )
      outcome.state should be(Some(initialState.copy(leaves = newLeaves, lastLeaves = newLeaves)))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "credit leaves when there is an inactive intimation and an inactive sabbatical privileged intimation" in withDriver { driver =>
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)
      val endDate = if (isWeekend(yesterday)) yesterday.minusDays(2) else yesterday
      val startDate = if (isWeekend(yesterday.minusDays(4))) yesterday.minusDays(6) else yesterday.minusDays(4)

      val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)

      val requestDate = if (isWeekend(today.minusDays(10))) today.minusDays(12) else today.minusDays(10)
      val requests = if (already5(today)) Set(Request(today, RequestType.Leave, RequestType.Leave)) else Set(Request(requestDate, RequestType.Leave, RequestType.Leave))
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))

      val is@initialState = state.copy(activeIntimationOpt = Some(activeIntimation), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreditLeaves(empId))

      val (earnedCredits, sickCredits) = computeCredits(initialState)
      val balanced = balanceExtra(is.leaves.earned + earnedCredits, is.leaves.currentYearEarned + earnedCredits, is.leaves.sick + sickCredits, is.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      outcome.events should ===(
        List(
          LastLeavesSaved(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(is.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(is.id, is.name, is.gender, is.doj, is.dor, is.designation, is.pfn, is.contactInfo, is.location, newLeaves, is.roles)
        )
      )
      outcome.state should be(Some(initialState.copy(leaves = newLeaves, lastLeaves = newLeaves)))
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    // Test cases for when an employee has already been released
    "invalidate adding an employee that already exists but has been released" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(AddEmployee(employee))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate release of an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId, dor))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "delete an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events should contain only EmployeeDeleted(e.id)
      outcome.state should ===(None)
      outcome.replies should contain only Done
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate updation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an intimation for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CancelIntimation(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate monthly credit of leaves for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(CreditLeaves(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }

    "invalidate yearly balancing of leaves for an already released employee" in withDriver { driver =>
      val today = LocalDate.now()
      val dor = if (isWeekend(today)) today.plusDays(2) else today

      val initialState = state.copy(dor = Some(dor))
      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(BalanceLeaves(empId))

      outcome.events.size should ===(0)
      outcome.state should be(Some(initialState))
      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.issues should be(Nil)
    }
  }
}
