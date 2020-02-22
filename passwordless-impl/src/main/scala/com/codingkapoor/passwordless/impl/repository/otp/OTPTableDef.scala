package com.codingkapoor.passwordless.impl.repository.otp

import java.time.ZonedDateTime

import slick.jdbc.MySQLProfile.api._
import com.codingkapoor.employee.api.models.Role
import com.codingkapoor.employee.api.models.Role.Role

final case class OTPEntity(otp: Int, empId: Long, email: String, roles: List[Role], createdAt: ZonedDateTime)

class OTPTableDef(tag: Tag) extends Table[OTPEntity](tag, "otps") {

  def email = column[String]("EMAIL", O.PrimaryKey)

  def otp = column[Int]("OTP")

  def empId = column[Long]("EMP_ID")

  implicit val rolesColumnType =
    MappedColumnType.base[List[Role], String]({ r => r.map(_.toString).mkString(",") }, { s => s.split(",").map(r => Role.withName(r)).toList })

  def roles = column[List[Role]]("ROLES")

  def createdAt = column[ZonedDateTime]("CREATED_AT")

  override def * =
    (otp, empId, email, roles, createdAt).mapTo[OTPEntity]
}

object OTPTableDef {
  lazy val otps = TableQuery[OTPTableDef]
}
