package com.codingkapoor.employee.impl

import com.codingkapoor.employee.api.models.Leaves
import com.codingkapoor.employee.impl.persistence.write.EmployeePersistenceEntity
import org.scalatest.{FlatSpec, Matchers}

class BalanceExtraSpec extends FlatSpec with Matchers {

  "balanceExtra" should "return balanced leaves when sick leaves is equal to extra leaves" in {
    EmployeePersistenceEntity.balanceExtra(1.5, 1.5, 0.5, 0.5) should ===(
      Leaves(earned = 1.5, currentYearEarned = 1.5)
    )
  }

   it should "return balanced leaves when extra leaves are more than sick leaves but less than or equal to earned leaves" in {
     EmployeePersistenceEntity.balanceExtra(1.5, 1.5, 0.5, 1) should ===(
       Leaves(earned = 1.0, currentYearEarned = 1.0)
     )
   }

  it should "return balanced leaves when extra leaves are more than both earned and sick leaves" in {
    EmployeePersistenceEntity.balanceExtra(1.5, 1.5, 0.5, 5) should ===(
      Leaves(extra = 3.0)
    )
  }

  it should "return balanced leaves when extra leaves are 0" in {
    EmployeePersistenceEntity.balanceExtra(12, 8, 2, 0) should ===(
      Leaves(earned = 12, currentYearEarned = 8, sick = 2)
    )
  }
}
