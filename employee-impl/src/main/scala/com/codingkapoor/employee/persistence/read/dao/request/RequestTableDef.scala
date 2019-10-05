package com.codingkapoor.employee.persistence.read.dao.request

import slick.jdbc.MySQLProfile.api._
import com.codingkapoor.employee.api.model.RequestType
import com.codingkapoor.employee.api.model.RequestType.RequestType
import com.codingkapoor.employee.persistence.read.dao.intimation.IntimationTableDef._

case class RequestEntity(date: Int, month: Int, year: Int, requestType: RequestType, intimationId: Long, id: Long = 0L)

class RequestTableDef(tag: Tag) extends Table[RequestEntity](tag, "request") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def date = column[Int]("DATE")

  def month = column[Int]("MONTH")

  def year = column[Int]("YEAR")

  implicit val requestTypeColumnType =
    MappedColumnType.base[RequestType, String]({ r => r.toString }, { s => RequestType.withName(s) })

  def requestType = column[RequestType]("REQUEST_TYPE")

  def intimationId = column[Long]("INTIMATION_ID")

  def intimationFK =
    foreignKey("INTIMATION_FK", intimationId, intimations)(_.id, onDelete = ForeignKeyAction.Cascade)

  override def * =
    (date, month, year, requestType, intimationId, id).mapTo[RequestEntity]
}

object RequestTableDef {
  lazy val requests = TableQuery[RequestTableDef]
}
