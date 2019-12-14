package com.codingkapoor.employee.persistence.read.repository.request

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import akka.Done

import scala.concurrent.ExecutionContext.Implicits.global

class RequestDao(db: Database) {
  val requests = RequestTableDef.requests

  def addRequest(request: RequestEntity): DBIO[Done] = {
    (requests += request).map(_ => Done)
  }

  def createTable: DBIO[Unit] = requests.schema.createIfNotExists

}
