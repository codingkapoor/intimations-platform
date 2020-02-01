package com.codingkapoor.employee.impl.service

import java.time.LocalDate

import akka.actor.ActorSystem

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.codingkapoor.employee.impl.persistence.read.repository.employee.EmployeeDao
import com.codingkapoor.employee.impl.persistence.write.{Credit, EmployeePersistenceEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

class CreditScheduler(system: ActorSystem, persistentEntityRegistry: PersistentEntityRegistry, employeeDao: EmployeeDao) {

  private def entityRef(id: Long) = persistentEntityRegistry.refFor[EmployeePersistenceEntity](id.toString)

  system.scheduler.schedule(15.minutes, 23.hours) {
    doCredits()
  }

  private def doCredits(): Unit = {
    val today = LocalDate.now()
    val lastDate = LocalDate.parse(s"${today.getYear}-${"%02d".format(today.getMonthValue)}-${today.lengthOfMonth}")

    if (today.isEqual(lastDate)) {
      val res = employeeDao.getEmployees()
      res.map { employees =>
        employees.filter(e => e.isActive).foreach(e => entityRef(e.id).ask(Credit(e.id)))
      }
    }
  }
}
