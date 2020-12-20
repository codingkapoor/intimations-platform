package com.iamsmkr.employee.impl.services

import java.time.LocalDate

import akka.actor.ActorSystem

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.iamsmkr.employee.impl.persistence.read.repositories.employee.EmployeeDao
import com.iamsmkr.employee.impl.persistence.write.EmployeePersistenceEntity
import com.iamsmkr.employee.impl.persistence.write.models.{BalanceLeaves, CreditLeaves}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Await

// TODO: Must be a singleton in a cluster
class CreditScheduler(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry, employeeDao: EmployeeDao) {

  private def entityRef(id: Long) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id.toString)

  system.scheduler.schedule(15.minutes, 1.day) {
    doCredits()
  }

  private def doCredits(): Unit = {
    val today = LocalDate.now()
    val lastDate = LocalDate.parse(s"${today.getYear}-${"%02d".format(today.getMonthValue)}-${today.lengthOfMonth}")

    val res = employeeDao.getEmployees()
    if (today.isEqual(lastDate)) {
      Await.result(res.map { employees =>
        employees.filterNot(e => e.dor.isDefined).foreach(e => entityRef(e.id).ask(CreditLeaves(e.id)))
      }, 10.seconds)
    }

    if (today.getMonthValue == 12 && today.getDayOfMonth == 31) {
      Await.result(res.map { employees =>
        employees.filterNot(e => e.dor.isDefined).foreach(e => entityRef(e.id).ask(BalanceLeaves(e.id)))
      }, 10.seconds)
    }
  }
}
