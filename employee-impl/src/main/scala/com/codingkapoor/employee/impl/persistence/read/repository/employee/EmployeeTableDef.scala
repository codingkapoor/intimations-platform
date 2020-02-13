package com.codingkapoor.employee.impl.persistence.read.repository.employee

import java.time.LocalDate

import com.codingkapoor.employee.api.model.Role
import com.codingkapoor.employee.api.model.Role.Role
import slick.jdbc.MySQLProfile.api._

case class EmployeeEntity(id: Long, name: String, gender: String, doj: LocalDate, dor: Option[LocalDate], designation: String,
                          pfn: String, phone: String, email: String, city: String, state: String, country: String,
                          earnedLeaves: Double, sickLeaves: Double, extraLeaves: Double, roles: List[Role])

class EmployeeTableDef(tag: Tag) extends Table[EmployeeEntity](tag, "employees") {

  def id = column[Long]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def gender = column[String]("GENDER")

  def doj = column[LocalDate]("DOJ")

  def dor = column[Option[LocalDate]]("DOR")

  def designation = column[String]("DESIGNATION")

  def pfn = column[String]("PFN", O.Length(64, varying = true))

  def phone = column[String]("PHONE")

  def email = column[String]("EMAIL")

  def city = column[String]("CITY")

  def state = column[String]("STATE")

  def country = column[String]("COUNTRY")

  def earnedLeaves = column[Double]("EARNED_LEAVES")

  def sickLeaves = column[Double]("SICK_LEAVES")

  def extraLeaves = column[Double]("EXTRA_LEAVES")

  implicit val rolesColumnType =
    MappedColumnType.base[List[Role], String]({ r => r.map(_.toString).mkString(",") }, { s => s.split(",").map(r => Role.withName(r)).toList })

  def roles = column[List[Role]]("ROLES")

  override def * =
    (id, name, gender, doj, dor, designation, pfn, phone, email, city, state, country, earnedLeaves, sickLeaves, extraLeaves, roles).mapTo[EmployeeEntity]
}

object EmployeeTableDef {
  lazy val employees = TableQuery[EmployeeTableDef]
}
