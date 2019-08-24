package com.codingkapoor.employee.persistence.read

import akka.Done
import com.codingkapoor.employee.api.Employee
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmployeeRepository(db: Database) {

  val employees = EmployeeTableDef.employees

  def addEmployee(employee: EmployeeEntity): DBIO[Done] = {
    (employees += employee).map(_ => Done)
  }

  def createTable: DBIO[Unit] = employees.schema.createIfNotExists
}
