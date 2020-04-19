package com.codingkapoor.employee.impl

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codingkapoor.employee.api.models.PrivilegedIntimationType.{Maternity, Paternity, Sabbatical}
import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, Intimation, IntimationReq, Leaves, Location, PrivilegedIntimation, PrivilegedIntimationType, Request, RequestType, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity.{already5, balanceExtra, between, computeCredits, getNewLeaves, isWeekend}
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, BalanceLeaves, CancelIntimation, CreateIntimation, CreditLeaves, DeleteEmployee, EmployeeAdded, EmployeeCommand, EmployeeDeleted, EmployeeEvent, EmployeeReleased, EmployeeState, EmployeeUpdated, IntimationCancelled, IntimationUpdated, LastLeavesSaved, LeavesCredited, PrivilegedIntimationCancelled, PrivilegedIntimationUpdated, ReleaseEmployee, UpdateEmployee, UpdateIntimation}
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
      val outcome = driver.run(ReleaseEmployee(empId))

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

    // Test cases when an employee is already added
    "invalidate release of an employee that has admin privilege" in withDriver { driver =>
      val e1 = employee.copy(roles = List(Role.Admin, Role.Employee))

      driver.run(AddEmployee(e1))

      val outcome = driver.run(ReleaseEmployee(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "release and credit leaves for an employee that has no ongoing intimations" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(ReleaseEmployee(empId))

      val (earnedCredits, sickCredits) = computeCredits(state)
      val balanced = balanceExtra(state.leaves.earned + earnedCredits, state.leaves.currentYearEarned + earnedCredits, state.leaves.sick + sickCredits, state.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val today = LocalDate.now()

      outcome.events should ===(
        List(
          LastLeavesSaved(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles),
          EmployeeReleased(state.id, today)
        )
      )
    }

    // Few dates already consumed would mean that active intimation would require to be updated instead of getting cancelled and
    // release date as one of requested dates would mean that active intimation update wouldn't render active intimation as inactive
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and has release date as one of the requested dates" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val requests =
        Set(
          Request(releaseDate.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate, RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newRequests = requests.filterNot(r => r.date.isAfter(releaseDate))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val iu = outcome.events.toList.head.asInstanceOf[IntimationUpdated]
      val outcomeIntimationUpdated = IntimationUpdated(iu.empId, iu.reason, iu.requests, now)

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(releaseDate) || already5(latestRequestDate)

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
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    // Few dates already consumed would mean that active intimation would require to be updated instead of getting cancelled and
    // release date as not one of requested dates would mean that active intimation update would render active intimation as inactive
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and not having release date as one of the requested dates" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val requests =
        Set(
          Request(releaseDate.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newRequests = requests.filterNot(r => r.date.isAfter(releaseDate))
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
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    // Release date as latest requested dates means that no update/cancel would be required for active intimation
    "release and credit leaves for an employee that has on ongoing active intimation that has few dates that are already consumed and having release date as the latest requested dates" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val requests =
        Set(
          Request(releaseDate.minusDays(2), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.minusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate, RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newRequests = requests.filterNot(r => r.date.isAfter(releaseDate))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(releaseDate) || already5(latestRequestDate)

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
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing active intimation that has first request date as release date" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val requests =
        Set(
          Request(releaseDate, RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newRequests = requests.filterNot(r => r.date.isAfter(releaseDate))
      val newLeaves = getNewLeaves(newRequests, lastLeaves = Leaves(initialState.lastLeaves.earned, initialState.lastLeaves.currentYearEarned, initialState.lastLeaves.sick, initialState.lastLeaves.extra))

      val now = LocalDateTime.now()
      val newState = initialState.copy(leaves = newLeaves, activeIntimationOpt = Some(Intimation(initialState.activeIntimationOpt.get.reason, newRequests, now)))

      val (earnedCredits, sickCredits) = computeCredits(newState)

      val latestRequestDate = newRequests.map(_.date).toList.sortWith(_.isBefore(_)).last
      val hasNoActiveIntimationAvailable = newState.activeIntimationOpt.isEmpty || latestRequestDate.isBefore(releaseDate) || already5(latestRequestDate)

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
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing active intimation that has all request dates in future" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val requests =
        Set(
          Request(releaseDate.plusDays(1), RequestType.Leave, RequestType.Leave),
          Request(releaseDate.plusDays(2), RequestType.Leave, RequestType.Leave)
        )
      val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))
      val initialState = state.copy(leaves = Leaves(extra = requests.size), activeIntimationOpt = Some(activeIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newRequests = requests.filterNot(r => r.date.isAfter(releaseDate))
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
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged maternity intimation that has few dates that are already consumed" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val startDate = releaseDate.minusDays(2)
      val endDate = releaseDate.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, releaseDate)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val newRequests = EmployeePersistenceEntity.between(startDate, releaseDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Maternity, startDate, releaseDate, s"$Maternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has few dates that are already consumed" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val startDate = releaseDate.minusDays(2)
      val endDate = releaseDate.plusDays(2)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, releaseDate)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val newRequests = EmployeePersistenceEntity.between(startDate, releaseDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val piu = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationUpdated]
      val outcomePrivilegedIntimationUpdated = PrivilegedIntimationUpdated(piu.empId, piu.privilegedIntimationType, piu.start, piu.end, piu.reason, piu.requests, now)

      outcomePrivilegedIntimationUpdated :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationUpdated(e.id, Paternity, startDate, releaseDate, s"$Paternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged maternity intimation that has all request dates in future" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val startDate = releaseDate.plusDays(2)
      val endDate = releaseDate.plusDays(8)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Maternity, startDate, releaseDate)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val newRequests = EmployeePersistenceEntity.between(startDate, releaseDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)

      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(e.id, Maternity, startDate, releaseDate, s"$Maternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    "release and credit leaves for an employee that has on ongoing privileged paternity intimation that has all request dates in future" in withDriver { driver =>
      val releaseDate = LocalDate.now()

      val startDate = releaseDate.plusDays(2)
      val endDate = releaseDate.plusDays(8)
      val extra = EmployeePersistenceEntity.between(startDate, endDate).filterNot(isWeekend).size

      val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
      val initialState = state.copy(leaves = Leaves(extra = extra), privilegedIntimationOpt = Some(privilegedIntimation))

      driver.initialize(Some(Some(initialState)))

      val outcome = driver.run(ReleaseEmployee(empId))

      val newState = initialState.copy(privilegedIntimationOpt = Some(PrivilegedIntimation(Paternity, startDate, releaseDate)))

      val (earnedCredits, sickCredits) = computeCredits(newState)
      val balanced = balanceExtra(newState.leaves.earned + earnedCredits, newState.leaves.currentYearEarned + earnedCredits, newState.leaves.sick + sickCredits, newState.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val newRequests = EmployeePersistenceEntity.between(startDate, releaseDate).filterNot(isWeekend).map(dt => Request(dt, RequestType.Leave, RequestType.Leave)).toSet

      val now = LocalDateTime.now()
      val pic = outcome.events.toList.head.asInstanceOf[PrivilegedIntimationCancelled]
      val outcomePrivilegedIntimationCancelled = PrivilegedIntimationCancelled(pic.empId, pic.privilegedIntimationType, pic.start, pic.end, pic.reason, pic.requests, now)

      outcomePrivilegedIntimationCancelled :: outcome.events.toList.tail should ===(
        List(
          PrivilegedIntimationCancelled(e.id, Paternity, startDate, releaseDate, s"$Paternity Leave", newRequests, now),
          LastLeavesSaved(newState.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(newState.id, newLeaves.earned, newLeaves.currentYearEarned, newLeaves.sick, newLeaves.extra),
          EmployeeUpdated(newState.id, newState.name, newState.gender, newState.doj, newState.dor, newState.designation, newState.pfn, newState.contactInfo, newState.location, newLeaves, newState.roles),
          EmployeeReleased(newState.id, releaseDate)
        )
      )
    }

    // Test cases for an employee that has already been released
    "invalidate adding an employee that already exists but has been released" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(AddEmployee(employee))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate updation of an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate release of an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(ReleaseEmployee(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate deletion of an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(DeleteEmployee(empId))

      outcome.events should contain only EmployeeDeleted(e.id)
      outcome.state should ===(None)
      outcome.issues should be(Nil)
    }

    "invalidate creation of an intimation for an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(CreateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate updation of an intimation for an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val intimationReq = IntimationReq("Travelling to my native", Set(Request(LocalDate.now(), RequestType.WFH, RequestType.Leave)))
      val outcome = driver.run(UpdateIntimation(empId, intimationReq))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate cancellation of an intimation for an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(CancelIntimation(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate monthly credit of leaves for an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(CreditLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "invalidate yearly balancing of leaves for an already released employee" in withDriver { driver =>
      driver.run(AddEmployee(employee))
      driver.run(ReleaseEmployee(empId))

      val outcome = driver.run(BalanceLeaves(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }
  }
}
