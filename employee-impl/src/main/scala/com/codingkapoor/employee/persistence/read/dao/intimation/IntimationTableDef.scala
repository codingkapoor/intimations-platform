package com.codingkapoor.employee.persistence.read.dao.intimation

import java.time.LocalDate

import slick.jdbc.MySQLProfile.api._
import com.codingkapoor.employee.persistence.read.dao.employee.EmployeeTableDef._

final case class IntimationEntity(empId: Long, reason: String, latestRequestDate: LocalDate, id: Long = 0L)

class IntimationTableDef(tag: Tag) extends Table[IntimationEntity](tag, "intimation") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def empId = column[Long]("EMP_ID")

  def reason = column[String]("REASON")

  def employeeFK =
    foreignKey("EMP_FK", empId, employees)(_.id, onDelete = ForeignKeyAction.Cascade)

  def latestRequestDate = column[LocalDate]("LATEST_REQUEST_DATE")

  override def * =
    (empId, reason, latestRequestDate, id).mapTo[IntimationEntity]
}

object IntimationTableDef {
  lazy val intimations = TableQuery[IntimationTableDef]
}
