package com.codingkapoor.passwordless.impl.repository.otp

import java.time.LocalDateTime
import slick.jdbc.MySQLProfile.api._

import com.codingkapoor.employee.api.model.Role
import com.codingkapoor.employee.api.model.Role.Role

final case class OTPEntity(otp: Int, empId: Long, email: String, roles: List[Role], createdAt: LocalDateTime)

class OTPTableDef(tag: Tag) extends Table[OTPEntity](tag, "otp") {

  def otp = column[Int]("OTP", O.PrimaryKey)

  def empId = column[Long]("EMP_ID")

  def email = column[String]("EMAIL")

  implicit val rolesColumnType =
    MappedColumnType.base[List[Role], String]({ r => r.map(_.toString).mkString(",") }, { s => s.split(",").map(r => Role.withName(r)).toList })

  def roles = column[List[Role]]("ROLES")

  def createdAt = column[LocalDateTime]("CREATED_AT")

  override def * =
    (otp, empId, email, roles, createdAt).mapTo[OTPEntity]
}

object OTPTableDef {
  lazy val otps = TableQuery[OTPTableDef]
}
