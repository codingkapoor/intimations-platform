package com.codingkapoor.employee.impl.persistence.read.repository.employee

import java.time.LocalDate
import slick.jdbc.MySQLProfile.api._

case class EmployeeEntity(id: Long, name: String, gender: String, doj: LocalDate, designation: String, pfn: String, isActive: Boolean,
                          phone: String, email: String, city: String, state: String, country: String, earnedLeaves: Int, sickLeaves: Int)

class EmployeeTableDef(tag: Tag) extends Table[EmployeeEntity](tag, "employee") {

  def id = column[Long]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def gender = column[String]("GENDER")

  def doj = column[LocalDate]("DOJ")

  def designation = column[String]("DESIGNATION")

  def pfn = column[String]("PFN", O.Unique, O.Length(64, varying = true))

  def isActive = column[Boolean]("IS_ACTIVE")

  def phone = column[String]("PHONE")

  def email = column[String]("EMAIL")

  def city = column[String]("CITY")

  def state = column[String]("STATE")

  def country = column[String]("COUNTRY")

  def earnedLeaves = column[Int]("EARNED_LEAVES")

  def sickLeaves = column[Int]("SICK_LEAVES")

  override def * =
    (id, name, gender, doj, designation, pfn, isActive, phone, email, city, state, country, earnedLeaves, sickLeaves).mapTo[EmployeeEntity]
}

object EmployeeTableDef {
  lazy val employees = TableQuery[EmployeeTableDef]
}
