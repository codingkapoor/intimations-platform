package com.codingkapoor.employee.persistence.read.dao.intimation

import java.time.LocalDate

import slick.jdbc.MySQLProfile.api._
import com.codingkapoor.employee.persistence.read.dao.employee.EmployeeTableDef._

case class IntimationEntity(id: Int, empId: String, reason: String, latestRequestDate: LocalDate)

class IntimationTableDef(tag: Tag) extends Table[IntimationEntity](tag, "intimation") {

  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

  def empId = column[String]("EMP_ID")

  def reason = column[String]("REASON")

  def employeeFK =
    foreignKey("EMP_FK", empId, employees)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def latestRequestDate = column[LocalDate]("LATEST_REQUEST_DATE")

  override def * =
    (id, empId, reason, latestRequestDate) <> (IntimationEntity.tupled, IntimationEntity.unapply)
}

object IntimationTableDef {
  val intimations = TableQuery[IntimationTableDef]
}
