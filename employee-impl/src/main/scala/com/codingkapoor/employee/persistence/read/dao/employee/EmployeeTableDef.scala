package com.codingkapoor.employee.persistence.read.dao.employee

import java.time.LocalDate
import slick.jdbc.MySQLProfile.api._

case class EmployeeEntity(id: String, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, earnedLeaves: Int, sickLeaves: Int)

class EmployeeTableDef(tag: Tag) extends Table[EmployeeEntity](tag, "employee") {

  def id = column[String]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def gender = column[String]("GENDER")

  def doj = column[LocalDate]("DOJ")

  def pfn = column[String]("PFN")

  def isActive = column[Boolean]("IS_ACTIVE")

  def earnedLeaves = column[Int]("EARNED_LEAVES")

  def sickLeaves = column[Int]("SICK_LEAVES")

  override def * =
    (id, name, gender, doj, pfn, isActive, earnedLeaves, sickLeaves) <> (EmployeeEntity.tupled, EmployeeEntity.unapply)
}

object EmployeeTableDef {
  val employees = TableQuery[EmployeeTableDef]
}
