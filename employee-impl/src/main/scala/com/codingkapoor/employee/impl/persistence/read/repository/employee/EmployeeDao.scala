package com.codingkapoor.employee.impl.persistence.read.repository.employee

import akka.Done
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmployeeDao(db: Database) {

  val employees = EmployeeTableDef.employees

  def addEmployee(employee: EmployeeEntity): DBIO[Done] = {
    (employees += employee).map(_ => Done)
  }

  def updateEmployee(employee: EmployeeEntity): DBIO[Done] = {
    employees.insertOrUpdate(employee).map(_ => Done)
  }

  def terminateEmployee(employee: EmployeeEntity): DBIO[Done] = {
    employees.insertOrUpdate(employee).map(_ => Done)
  }

  def getEmployees(email: Option[String]): Future[Seq[EmployeeEntity]] = {
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
