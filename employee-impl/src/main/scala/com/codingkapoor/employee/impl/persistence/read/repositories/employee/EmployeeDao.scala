package com.codingkapoor.employee.impl.persistence.read.repositories.employee

import java.time.LocalDate

import akka.Done
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class EmployeeDao(db: Database) {

  val employees = EmployeeTableDef.employees

  def addEmployee(employee: EmployeeEntity): DBIO[Done] = {
    (employees += employee).map(_ => Done)
  }

  def updateEmployee(employee: EmployeeEntity): DBIO[Done] = {
    employees.insertOrUpdate(employee).map(_ => Done)
  }

  def releaseEmployee(id: Long, dor: LocalDate): DBIO[Done] = {
    val res = Await.result(getEmployee(id), 5.seconds)

    if (res.isDefined) employees.insertOrUpdate(res.get.copy(dor = Some(dor))).map(_ => Done)
    else throw new Exception(s"Employee not found with id $id")
  }

  def getEmployees(email: Option[String] = None): Future[Seq[EmployeeEntity]] = {
    if (email.isDefined) db.run(employees.filter(_.email === email.get).result)
    else db.run(employees.result)
  }

  def getEmployee(id: Long): Future[Option[EmployeeEntity]] = {
    db.run(employees.filter(_.id === id).result.headOption)
  }

  def deleteEmployee(id: Long): DBIO[Done] = {
    (employees.filter(_.id === id).delete).map(_ => Done)
  }

  def createTable: DBIO[Unit] = employees.schema.createIfNotExists
}
