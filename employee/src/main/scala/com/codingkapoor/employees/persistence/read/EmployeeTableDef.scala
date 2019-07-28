package com.codingkapoor.employees.persistence.read

import java.time.LocalDate
import slick.jdbc.MySQLProfile.api._

case class EmployeeReadEntity(id: String, name: String, gender: String, doj: LocalDate, pfn: String)

class EmployeeTableDef(tag: Tag) extends Table[EmployeeReadEntity](tag, "EMPLOYEE") {

  def id = column[String]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def gender = column[String]("GENDER")

  def doj = column[LocalDate]("DOJ")

  def pfn = column[String]("PFN")

  override def * =
    (id, name, gender, doj, pfn) <> (EmployeeReadEntity.tupled, EmployeeReadEntity.unapply)
}

object EmployeeTableDef {
  val employees = TableQuery[EmployeeTableDef]
}
