package com.codingkapoor.employee.impl.persistence.read.repositories.request

import java.time.LocalDate

import slick.jdbc.MySQLProfile.api._
import com.codingkapoor.employee.api.models.RequestType
import com.codingkapoor.employee.api.models.RequestType.RequestType
import com.codingkapoor.employee.impl.persistence.read.repositories.intimation.IntimationTableDef._

case class RequestEntity(date: LocalDate, firstHalf: RequestType, secondHalf: RequestType, intimationId: Long, id: Long = 0L)

class RequestTableDef(tag: Tag) extends Table[RequestEntity](tag, "requests") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def date = column[LocalDate]("DATE")

  implicit val requestTypeColumnType =
    MappedColumnType.base[RequestType, String]({ r => r.toString }, { s => RequestType.withName(s) })

  def firstHalf = column[RequestType]("FIRST_HALF")

  def secondHalf = column[RequestType]("SECOND_HALF")

  def intimationId = column[Long]("INTIMATION_ID")

  def intimationFK =
    foreignKey("INTIMATION_FK", intimationId, intimations)(_.id, onDelete = ForeignKeyAction.Cascade)

  override def * =
    (date, firstHalf, secondHalf, intimationId, id).mapTo[RequestEntity]
}

object RequestTableDef {
  lazy val requests = TableQuery[RequestTableDef]
}
