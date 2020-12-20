package com.iamsmkr.employee.impl

import com.iamsmkr.employee.api.models.Leaves
import com.iamsmkr.employee.impl.persistence.write.EmployeePersistenceEntity
import org.scalatest.{FlatSpec, Matchers}

class BalanceExtraWithNewCreditsSpec extends FlatSpec with Matchers {

  "balanceExtraWithNewCredits" should "return balanced leaves when credited sick leaves is equal to extra leaves" in {
    EmployeePersistenceEntity.balanceExtraWithNewCredits(1.5, 1.5, 0.5, 0.5) should ===(
      Leaves(earned = 1.5, currentYearEarned = 1.5)
    )
  }

   it should "return balanced leaves when extra leaves are more than credited sick leaves but less than or equal to credited earned leaves" in {
     EmployeePersistenceEntity.balanceExtraWithNewCredits(1.5, 1.5, 0.5, 1) should ===(
       Leaves(earned = 1.0, currentYearEarned = 1.0)
     )
   }

  it should "return balanced leaves when extra leaves are more than both credited earned and sick leaves" in {
    EmployeePersistenceEntity.balanceExtraWithNewCredits(1.5, 1.5, 0.5, 5) should ===(
      Leaves(extra = 3.0)
    )
  }

  it should "return balanced leaves when extra leaves are 0" in {
    EmployeePersistenceEntity.balanceExtraWithNewCredits(12.5, 8, 2.5, 0) should ===(
      Leaves(earned = 12.5, currentYearEarned = 8, sick = 2.5)
    )
  }

  it should "return balanced leaves when credited leaves are 0" in {
    EmployeePersistenceEntity.balanceExtraWithNewCredits(0, 0, 0, 5.0) should ===(
      Leaves(extra = 5.0)
    )
  }
}
