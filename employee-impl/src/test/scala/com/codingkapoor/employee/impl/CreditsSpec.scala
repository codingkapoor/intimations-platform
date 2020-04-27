package com.codingkapoor.employee.impl

import java.time.LocalDate

import com.codingkapoor.employee.api.models.{ContactInfo, Employee, EmployeeInfo, Leaves, Location, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity
import com.codingkapoor.employee.impl.persistence.write.models.EmployeeState
import org.scalatest.FlatSpec

class CreditsSpec extends FlatSpec {

  val empId = 128L
  val e@employee = Employee(empId, "John Doe", "M", LocalDate.parse("2018-01-16"), None, "System Engineer", "PFN001",
    ContactInfo("+91-9912345678", "mail@johndoe.com"), Location(), Leaves(), List(Role.Employee))
  val state: EmployeeState = EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, Leaves(), e.roles, None, None, Leaves())

  "computeCredits" should "return earned and sick leaves as 1.5 and 0.5 respectively when there is no active or privileged intimation and neither doj or dor fall in the current month" in {
    val (el, sl) = EmployeePersistenceEntity.computeCredits(state)
    assert(el == 1.5 && sl == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is no active or privileged intimation and neither does dor fall in the current month, doj however falls between 1st and 10th day of the current month" in {
    val today = LocalDate.now()
    val doj = LocalDate.parse(s"${today.getYear}-${"%02d".format(today.getMonthValue)}-10")

    val (el, sl) = EmployeePersistenceEntity.computeCredits(state.copy(doj = doj))
    assert(el == 1.5 && sl == 0.5)
  }

  it should "return earned and sick leaves as 1.0 and 0.5 respectively when there is no active or privileged intimation and neither does dor fall in the current month, doj however falls between 11th and 16th day of the current month" in {
    val today = LocalDate.now()
    val doj = LocalDate.parse(s"${today.getYear}-${"%02d".format(today.getMonthValue)}-16")

    val (el, sl) = EmployeePersistenceEntity.computeCredits(state.copy(doj = doj))
    assert(el == 1.0 && sl == 0.5)
  }

}
