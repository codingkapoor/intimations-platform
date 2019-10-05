package com.codingkapoor.employee.persistence.read.dao.employee

import java.time.LocalDate
import slick.jdbc.MySQLProfile.api._

case class EmployeeEntity(id: Long, name: String, gender: String, doj: LocalDate, pfn: String, isActive: Boolean, earnedLeaves: Int, sickLeaves: Int)

class EmployeeTableDef(tag: Tag) extends Table[EmployeeEntity](tag, "employee") {

  def id = column[Long]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def gender = column[String]("GENDER")

  def doj = column[LocalDate]("DOJ")

  def pfn = column[String]("PFN", O.Unique, O.Length(64, varying = true))

  def isActive = column[Boolean]("IS_ACTIVE")

  def earnedLeaves = column[Int]("EARNED_LEAVES")

  def sickLeaves = column[Int]("SICK_LEAVES")

  override def * =
    (id, name, gender, doj, pfn, isActive, earnedLeaves, sickLeaves).mapTo[EmployeeEntity]
}

object EmployeeTableDef {
  lazy val employees = TableQuery[EmployeeTableDef]
}
