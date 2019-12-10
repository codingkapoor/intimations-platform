package com.codingkapoor.employee.persistence.read.dao.intimation

import java.time.{LocalDate, LocalDateTime}
import slick.jdbc.MySQLProfile.api._

import com.codingkapoor.employee.persistence.read.dao.employee.EmployeeTableDef._

final case class IntimationEntity(empId: Long, reason: String, latestRequestDate: LocalDate, lastModified: LocalDateTime, id: Long = 0L)

class IntimationTableDef(tag: Tag) extends Table[IntimationEntity](tag, "intimation") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def empId = column[Long]("EMP_ID")

  def reason = column[String]("REASON")

  def employeeFK =
    foreignKey("EMP_FK", empId, employees)(_.id, onDelete = ForeignKeyAction.Cascade)

  def latestRequestDate = column[LocalDate]("LATEST_REQUEST_DATE")

  def lastModified = column[LocalDateTime]("LAST_MODIFIED")

  override def * =
    (empId, reason, latestRequestDate, lastModified, id).mapTo[IntimationEntity]
}

object IntimationTableDef {
  lazy val intimations = TableQuery[IntimationTableDef]
}
