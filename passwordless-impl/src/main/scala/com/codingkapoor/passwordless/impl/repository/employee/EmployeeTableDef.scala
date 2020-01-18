package com.codingkapoor.passwordless.impl.repository.employee

import com.codingkapoor.employee.api.model.Role
import com.codingkapoor.employee.api.model.Role.Role
import slick.jdbc.MySQLProfile.api._

case class EmployeeEntity(id: Long, name: String, isActive: Boolean, email: String, roles: List[Role])

class EmployeeTableDef(tag: Tag) extends Table[EmployeeEntity](tag, "employees") {

  def id = column[Long]("ID", O.PrimaryKey)

  def name = column[String]("NAME")

  def isActive = column[Boolean]("IS_ACTIVE")

  def email = column[String]("EMAIL")

  implicit val rolesColumnType =
    MappedColumnType.base[List[Role], String]({ r => r.map(_.toString).mkString(",") }, { s => s.split(",").map(r => Role.withName(r)).toList })

  def roles = column[List[Role]]("ROLES")

  override def * =
    (id, name, isActive, email, roles).mapTo[EmployeeEntity]
}

object EmployeeTableDef {
  lazy val employees = TableQuery[EmployeeTableDef]
}
