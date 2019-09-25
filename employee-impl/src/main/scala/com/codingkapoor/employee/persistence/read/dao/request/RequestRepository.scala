package com.codingkapoor.employee.persistence.read.dao.request

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import akka.Done

import scala.concurrent.ExecutionContext.Implicits.global

class RequestRepository(db: Database) {
  val requests = RequestTableDef.requests

  def addRequest(request: RequestEntity): DBIO[Done] = {
    (requests += request).map(_ => Done)
  }

  def deleteRequests(intimationId: Int): DBIO[Done] = {
    requests
      .filter(i => i.intimationId === intimationId)
      .delete
      .map(_ => Done)
  }

  def createTable: DBIO[Unit] = requests.schema.createIfNotExists

}
