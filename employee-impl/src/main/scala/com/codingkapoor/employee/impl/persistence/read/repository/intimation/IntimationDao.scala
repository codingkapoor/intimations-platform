package com.codingkapoor.employee.impl.persistence.read.repository.intimation

import java.time.LocalDate
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import akka.Done
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.codingkapoor.employee.impl.persistence.read.repository.employee.{EmployeeEntity, EmployeeTableDef}
import com.codingkapoor.employee.impl.persistence.read.repository.request.{RequestEntity, RequestTableDef}

class IntimationDao(db: Database) {
  val employees = EmployeeTableDef.employees
  val intimations = IntimationTableDef.intimations
  val requests = RequestTableDef.requests

  def createIntimation(intimation: IntimationEntity): DBIO[Long] = {
    (intimations returning intimations.map(_.id)) += intimation
  }

  def getActiveIntimations: Future[Seq[((EmployeeEntity, IntimationEntity), RequestEntity)]] = {
    db.run(
      employees
        .join(intimations).on(_.id === _.empId)
        .join(requests).on(_._2.id === _.intimationId)
        .filter { case ((_, i), _) => i.latestRequestDate >= LocalDate.now() }
        .result
    )
  }

  def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): Future[Seq[(IntimationEntity, RequestEntity)]] = {
    db.run(
      intimations
        .join(requests).on(_.id === _.intimationId)
        .filter { case (i, r) => i.empId === empId && r.date >= start && r.date <= end }
        .result
    )
  }

  def deleteIntimation(empId: Long): DBIO[Done] = {
    intimations
      .filter(i => i.empId === empId && i.latestRequestDate >= LocalDate.now())
      .delete
      .map(_ => Done)
  }

  def deleteAllIntimations(empId: Long): DBIO[Done] = {
    intimations
      .filter(i => i.empId === empId)
      .delete
      .map(_ => Done)
  }

  def createTable: DBIO[Unit] = intimations.schema.createIfNotExists

}
