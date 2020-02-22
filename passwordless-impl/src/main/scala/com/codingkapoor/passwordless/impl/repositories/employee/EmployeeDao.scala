package com.codingkapoor.passwordless.impl.repositories.employee

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EmployeeDao(db: Database) {

  val employees = EmployeeTableDef.employees

  createTable

  def addEmployee(employee: EmployeeEntity): Future[Int] = {
    db.run(employees += employee)
  }

  def updateEmployee(employee: EmployeeEntity): Future[Int] = {
    db.run(employees.insertOrUpdate(employee))
  }

  def getEmployees(email: Option[String]): Future[Seq[EmployeeEntity]] = {
    if (email.isDefined) db.run(employees.filter(_.email === email.get).result)
    else db.run(employees.result)
  }

  def getEmployee(id: Long): Future[Option[EmployeeEntity]] = {
    db.run(employees.filter(_.id === id).result.headOption)
  }

  def deleteEmployee(id: Long): Future[Int] = {
    db.run(employees.filter(_.id === id).delete)
  }

  private def createTable: Future[Unit] = db.run(employees.schema.createIfNotExists)
}
