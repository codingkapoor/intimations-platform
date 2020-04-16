package com.codingkapoor.employee.impl

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, IntimationReq, Leaves, Location, Request, RequestType, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity.{balanceExtra, computeCredits}
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, BalanceLeaves, CancelIntimation, CreateIntimation, CreditLeaves, DeleteEmployee, EmployeeAdded, EmployeeCommand, EmployeeDeleted, EmployeeEvent, EmployeeReleased, EmployeeState, EmployeeUpdated, LastLeavesSaved, LeavesCredited, ReleaseEmployee, UpdateEmployee, UpdateIntimation}
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
      val e1 = e.copy(roles = List(Role.Admin, Role.Employee))

      driver.run(AddEmployee(e1))

      val outcome = driver.run(ReleaseEmployee(empId))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }

    "release and credit leaves for an employee that has no ongoing intimations" in withDriver { driver =>
      driver.run(AddEmployee(employee))

      val outcome = driver.run(ReleaseEmployee(empId))

      val state = EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles, None, None, Leaves())

      val (earnedCredits, sickCredits) = EmployeePersistenceEntity.computeCredits(state)
      val balanced = balanceExtra(state.leaves.earned + earnedCredits, state.leaves.currentYearEarned + earnedCredits, state.leaves.sick + sickCredits, state.leaves.extra)
      val newLeaves = Leaves(balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra)

      val today = LocalDate.now()

      outcome.events should ===(
        List(
          LastLeavesSaved(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          LeavesCredited(state.id, balanced.earned, balanced.currentYearEarned, balanced.sick, balanced.extra),
          EmployeeUpdated(state.id, state.name, state.gender, state.doj, state.dor, state.designation, state.pfn, state.contactInfo, state.location, newLeaves, state.roles),
          EmployeeReleased(state.id, today))
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
