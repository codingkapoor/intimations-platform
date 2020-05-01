package com.codingkapoor.employee.impl

import java.time.LocalDate

import com.codingkapoor.employee.api.models.{Leaves, Request, RequestType}
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity
import org.scalatest.{FlatSpec, Matchers}

class GetNewLeavesSpec extends FlatSpec with Matchers {

  "getNewLeaves" should "return new leaves when sick leaves is equal to applied leaves" in {
    val dummyDate = LocalDate.now()
    val requests =
      Set(
        Request(dummyDate, RequestType.Leave, RequestType.Leave),
        Request(dummyDate, RequestType.WFO, RequestType.Leave),
        Request(dummyDate, RequestType.Leave, RequestType.WFH)
      )

    EmployeePersistenceEntity.getNewLeaves(requests, Leaves(earned = 20.0, currentYearEarned = 12.0, sick = 3.0)) should ===(
      Leaves(earned = 20.0, currentYearEarned = 12.0, sick = 1.0)
    )
  }

  it should "return new leaves when applied leaves are more than sick leaves but less than or equal to earned leaves" in {
    val dummyDate = LocalDate.now()
    val requests =
      Set(
        Request(dummyDate, RequestType.Leave, RequestType.Leave),
        Request(dummyDate, RequestType.WFO, RequestType.Leave),
        Request(dummyDate, RequestType.Leave, RequestType.WFO),
        Request(dummyDate, RequestType.Leave, RequestType.WFH)
      )

    EmployeePersistenceEntity.getNewLeaves(requests, Leaves(earned = 5.0, currentYearEarned = 3.0, sick = 1.0)) should ===(
      Leaves(earned = 3.5, currentYearEarned = 1.5)
    )
  }

  it should "return new leaves when applied leaves are more than both earned and sick leaves" in {
    val dummyDate = LocalDate.now()
    val requests =
      Set(
        Request(dummyDate, RequestType.Leave, RequestType.Leave),
        Request(dummyDate, RequestType.WFO, RequestType.Leave),
        Request(dummyDate, RequestType.Leave, RequestType.WFO),
        Request(dummyDate, RequestType.Leave, RequestType.WFH)
      )

    EmployeePersistenceEntity.getNewLeaves(requests, Leaves(extra = 5.0)) should ===(
      Leaves(extra = 7.5)
    )
  }

  it should "return new leaves when applied leaves are 0" in {
    val requests = Set[Request]()

    EmployeePersistenceEntity.getNewLeaves(requests, Leaves(extra = 5.0)) should ===(
      Leaves(extra = 5.0)
    )
  }
}
