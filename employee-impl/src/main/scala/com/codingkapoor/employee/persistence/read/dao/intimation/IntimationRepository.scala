package com.codingkapoor.employee.persistence.read.dao.intimation

import java.time.LocalDate

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import akka.Done
import com.codingkapoor.employee.persistence.read.dao.request.{RequestEntity, RequestTableDef}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class IntimationRepository(db: Database) {
  val intimations = IntimationTableDef.intimations
  val requests = RequestTableDef.requests

  def createIntimation(intimation: IntimationEntity): DBIO[Long] = {
    (intimations returning intimations.map(_.id)) += intimation
  }

  def getActiveIntimation(empId: String): DBIO[Option[IntimationEntity]] = {
    intimations.filter(i => i.empId === empId && i.latestRequestDate >= LocalDate.now()).result.headOption
  }

  def getActiveIntimations: Future[Seq[(IntimationEntity, RequestEntity)]] = {
    db.run(
      intimations
        .join(requests).on(_.id === _.intimationId)
        .filter { case (i, _) => i.latestRequestDate >= LocalDate.now() }
        .result
    )
  }

  def getIntimations(empId: String, month: Int, year: Int): Future[Seq[(IntimationEntity, RequestEntity)]] = {
    db.run(
      intimations
        .join(requests).on(_.id === _.intimationId)
        .filter { case (i, r) => i.empId === empId && r.month === month && r.year === year }
        .result
    )
  }

  def deleteIntimation(empId: String): DBIO[Done] = {
    intimations
      .filter(i => i.empId === empId && i.latestRequestDate >= LocalDate.now())
      .delete
      .map(_ => Done)
  }

  def deleteAllIntimations(empId: String): DBIO[Done] = {
    intimations
      .filter(i => i.empId === empId)
      .delete
      .map(_ => Done)
  }

  def createTable: DBIO[Unit] = intimations.schema.createIfNotExists

}
