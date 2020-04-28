package com.codingkapoor.employee.impl

import java.time.LocalDate

import com.codingkapoor.employee.api.models.{ContactInfo, Employee, Leaves, Location, Role}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity
import com.codingkapoor.employee.impl.persistence.write.models.EmployeeState
import org.scalatest.FlatSpec

class CreditsSpec extends FlatSpec {

  val empId = 128L
  val e@employee = Employee(empId, "John Doe", "M", LocalDate.parse("2018-01-01"), None, "System Engineer", "PFN001",
    ContactInfo("+91-9912345678", "mail@johndoe.com"), Location(), Leaves(), List(Role.Employee))
  val state: EmployeeState = EmployeeState(e.id, e.name, e.gender, e.doj, e.dor, e.designation, e.pfn, e.contactInfo, e.location, Leaves(), e.roles, None, None, Leaves())

  "computeCredits" should "return earned and sick leaves as 1.5 and 0.5 respectively when there is no active or privileged intimation and neither does doj or dor fall in the current month" in {
    val (el, sl) = EmployeePersistenceEntity.computeCreditsForYearMonth(state, state.doj.getMonthValue, state.doj.getYear)
    assert(el == 1.5 && sl == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 21 or more days and total number of days in a month is 28" in {
    val doj1 = LocalDate.parse(s"2019-02-01")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 1.5 && sl1 == 0.5)

    val doj2 = LocalDate.parse(s"2019-02-08")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 1.5 && sl2 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 21 or more days and total number of days in a month is 29" in {
    val doj3 = LocalDate.parse(s"2020-02-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse(s"2020-02-09")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 21 or more days and total number of days in a month is 30" in {
    val doj3 = LocalDate.parse(s"2020-04-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse(s"2020-04-10")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 21 or more days and total number of days in a month is 31" in {
    val doj3 = LocalDate.parse(s"2020-01-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse(s"2020-01-11")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.0 and 0.5 respectively when prorata number of days computes to 16 or more days" in {
    val doj1 = LocalDate.parse(s"2020-02-10")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 1.0 && sl1 == 0.5)

    val doj2 = LocalDate.parse(s"2020-02-14")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 1.0 && sl2 == 0.5)
  }

}
