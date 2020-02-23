package com.codingkapoor.employee.impl

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, Leaves, Location, Role}
import com.codingkapoor.employee.impl.persistence.write.{EmployeePersistenceEntity, EmployeeSerializerRegistry}
import com.codingkapoor.employee.impl.persistence.write.models.{AddEmployee, EmployeeAdded, EmployeeCommand, EmployeeEvent, EmployeeState, UpdateEmployee}
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
    ContactInfo("+91-9912345678", "mail@johndoe.com"), Location(), Leaves(), List(Role.Employee, Role.Admin))
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

    "add employee when no employee found with the given employee id" in withDriver { driver =>
      val outcome = driver.run(AddEmployee(employee))

      outcome.events should contain only EmployeeAdded(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles)
      outcome.state should ===(Some(EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, e.leaves, e.roles, None, Leaves())))
      outcome.issues should be(Nil)
    }

    "invalidate update employee when no employee found with the given employee id" in withDriver { driver =>
      val outcome = driver.run(UpdateEmployee(empId, employeeInfo))

      outcome.replies.head.getClass should be(classOf[InvalidCommandException])
      outcome.events.size should ===(0)
      outcome.issues should be(Nil)
    }
  }

}
