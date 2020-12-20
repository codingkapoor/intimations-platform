package com.iamsmkr.employee.impl

import java.time.{LocalDate, LocalDateTime}

import com.iamsmkr.employee.api.models.PrivilegedIntimationType._
import com.iamsmkr.employee.api.models.{ContactInfo, Employee, Intimation, Leaves, Location, PrivilegedIntimation, Request, RequestType, Role}
import com.iamsmkr.employee.impl.persistence.write.EmployeePersistenceEntity
import com.iamsmkr.employee.impl.persistence.write.models.EmployeeState
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

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 20 or more days and total number of days in a month is 28" in {
    val doj1 = LocalDate.parse("2019-02-01")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 1.5 && sl1 == 0.5)

    val doj2 = LocalDate.parse("2019-02-09")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 1.5 && sl2 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 20 or more days and total number of days in a month is 29" in {
    val doj3 = LocalDate.parse("2020-02-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse("2020-02-10")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 20 or more days and total number of days in a month is 30" in {
    val doj3 = LocalDate.parse("2020-04-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse("2020-04-11")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when prorata number of days computes to 20 or more days and total number of days in a month is 31" in {
    val doj3 = LocalDate.parse("2020-01-01")

    val (el3, sl3) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj3), doj3.getMonthValue, doj3.getYear)
    assert(el3 == 1.5 && sl3 == 0.5)

    val doj4 = LocalDate.parse("2020-01-12")

    val (el4, sl4) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj4), doj4.getMonthValue, doj4.getYear)
    assert(el4 == 1.5 && sl4 == 0.5)
  }

  it should "return earned and sick leaves as 1.0 and 0.5 respectively when prorata number of days computes to 15 or more days" in {
    val doj1 = LocalDate.parse("2020-02-11")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 1.0 && sl1 == 0.5)

    val doj2 = LocalDate.parse("2020-02-14")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 1.0 && sl2 == 0.5)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when prorata number of days computes to 10 or more days" in {
    val doj1 = LocalDate.parse("2020-04-17")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 0.5 && sl1 == 0)

    val doj2 = LocalDate.parse("2020-04-21")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 0.5 && sl2 == 0)
  }

  it should "return earned and sick leaves as 0 and 0 respectively when prorata number of days computes to less than 10 days" in {
    val doj1 = LocalDate.parse("2020-01-23")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj1), doj1.getMonthValue, doj1.getYear)
    assert(el1 == 0 && sl1 == 0)

    val doj2 = LocalDate.parse("2020-01-31")

    val (el2, sl2) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj2), doj2.getMonthValue, doj2.getYear)
    assert(el2 == 0 && sl2 == 0)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when an employee joined and was released the very same month" in {
    val doj = LocalDate.parse("2020-01-06")
    val dor = LocalDate.parse("2020-01-27")

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(state.copy(doj = doj, dor = Some(dor)), doj.getMonthValue, doj.getYear)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively while ignoring any active intimation" in {
    val requests =
      Set(
        Request(LocalDate.parse("2020-02-26"), RequestType.Leave, RequestType.Leave),
        Request(LocalDate.parse("2020-02-27"), RequestType.Leave, RequestType.Leave),
        Request(LocalDate.parse("2020-02-28"), RequestType.Leave, RequestType.Leave),
        Request(LocalDate.parse("2020-03-02"), RequestType.Leave, RequestType.Leave),
        Request(LocalDate.parse("2020-03-03"), RequestType.Leave, RequestType.Leave)
      )
    val activeIntimation = Intimation("Visiting my native", requests, LocalDateTime.parse("2020-01-12T10:15:30"))

    val initialState = state.copy(activeIntimationOpt = Some(activeIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 2, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively while ignoring any privileged paternity intimation" in {
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-04-28")
    val privilegedIntimation = PrivilegedIntimation(Paternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively while ignoring any inactive privileged intimation" in {
    val startDate = LocalDate.parse("2020-02-12")
    val endDate = LocalDate.parse("2020-02-28")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively while ignoring any privileged intimation in the future" in {
    val startDate = LocalDate.parse("2020-08-12")
    val endDate = LocalDate.parse("2020-08-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 0 and 0 respectively when there is an ongoing sabbatical privileged intimation" in {
    val startDate = LocalDate.parse("2020-03-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it starts in the current month but ends in the next month" in {
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it starts in the current month but ends in the next month and also that the employee is released in the current month" in {
    val dor = LocalDate.parse("2020-04-25")
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing sabbatical privileged intimation such that it starts in the current month but ends in the next month and also that the employee joined in the current month" in {
    val doj = LocalDate.parse("2020-04-03")
    val startDate = LocalDate.parse("2020-04-28")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(doj = doj, privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing sabbatical privileged intimation such that it starts in the current month but ends in the next month and also that the employee joined and is released in the current month" in {
    val doj = LocalDate.parse("2020-04-03")
    val dor = LocalDate.parse("2020-04-28")
    val startDate = LocalDate.parse("2020-04-25")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(doj = doj, dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it started in the last month but ends in the current month" in {
    val startDate = LocalDate.parse("2020-03-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing sabbatical privileged intimation such that it starts and ends in the current month" in {
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it starts and ends in the current month and also that the employee joined in the current month" in {
    val doj = LocalDate.parse("2020-04-10")
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(doj = doj, privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it starts and ends in the current month and also that the employee is released by the end of the month" in {
    val dor = LocalDate.parse("2020-04-26")
    val startDate = LocalDate.parse("2020-04-08")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing sabbatical privileged intimation such that it starts and ends in the current month and also that the employee joined and is released in the current month" in {
    val doj = LocalDate.parse("2020-04-02")
    val dor = LocalDate.parse("2020-04-26")
    val startDate = LocalDate.parse("2020-04-08")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Sabbatical, startDate, endDate)
    val initialState = state.copy(doj = doj, dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0 and 0 respectively when there is an ongoing maternity privileged intimation" in {
    val startDate = LocalDate.parse("2020-03-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it starts in the current month but ends in the next month" in {
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it starts in the current month but ends in the next month and also that the employee is released in the current month" in {
    val dor = LocalDate.parse("2020-04-25")
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing maternity privileged intimation such that it starts in the current month but ends in the next month and also that the employee joined in the current month" in {
    val doj = LocalDate.parse("2020-04-03")
    val startDate = LocalDate.parse("2020-04-28")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(doj = doj, privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing maternity privileged intimation such that it starts in the current month but ends in the next month and also that the employee joined and is released in the current month" in {
    val doj = LocalDate.parse("2020-04-03")
    val dor = LocalDate.parse("2020-04-28")
    val startDate = LocalDate.parse("2020-04-25")
    val endDate = LocalDate.parse("2020-05-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(doj = doj, dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it started in the last month but ends in the current month" in {
    val startDate = LocalDate.parse("2020-03-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 1.5 and 0.5 respectively when there is an ongoing maternity privileged intimation such that it starts and ends in the current month" in {
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 1.5 && sl1 == 0.5)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it starts and ends in the current month and also that the employee joined in the current month" in {
    val doj = LocalDate.parse("2020-04-10")
    val startDate = LocalDate.parse("2020-04-12")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(doj = doj, privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it starts and ends in the current month and also that the employee is released by the end of the month" in {
    val dor = LocalDate.parse("2020-04-26")
    val startDate = LocalDate.parse("2020-04-08")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

  it should "return earned and sick leaves as 0.5 and 0 respectively when there is an ongoing maternity privileged intimation such that it starts and ends in the current month and also that the employee joined and is released in the current month" in {
    val doj = LocalDate.parse("2020-04-02")
    val dor = LocalDate.parse("2020-04-26")
    val startDate = LocalDate.parse("2020-04-08")
    val endDate = LocalDate.parse("2020-04-20")
    val privilegedIntimation = PrivilegedIntimation(Maternity, startDate, endDate)
    val initialState = state.copy(doj = doj, dor = Some(dor), privilegedIntimationOpt = Some(privilegedIntimation))

    val (el1, sl1) = EmployeePersistenceEntity.computeCreditsForYearMonth(initialState, 4, 2020)
    assert(el1 == 0.5 && sl1 == 0)
  }

}
