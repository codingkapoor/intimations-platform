package com.codingkapoor.notifier.impl.repositories.employee

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

  def getEmployee(empId: Long): Future[Option[EmployeeEntity]] = {
    db.run(employees.filter(_.empId === empId).result.headOption)
  }

  def getEmployees: Future[List[EmployeeEntity]] = {
    db.run(employees.result).map(_.toList)
  }

  def updateEmployee(employee: EmployeeEntity): Future[Int] = {
    db.run(employees.insertOrUpdate(employee))
  }

  def deleteEmployee(empId: Long): Future[Int] = {
    db.run(employees.filter(_.empId === empId).delete)
  }

  private def createTable: Future[Unit] = db.run(employees.schema.createIfNotExists)
}
