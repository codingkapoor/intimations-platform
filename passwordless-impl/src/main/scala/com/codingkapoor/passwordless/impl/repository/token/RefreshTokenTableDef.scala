package com.codingkapoor.passwordless.impl.repository.token

import java.time.LocalDateTime
import slick.jdbc.MySQLProfile.api._

final case class RefreshTokenEntity(refreshToken: String, empId: Long, email: String, createdAt: LocalDateTime)

class RefreshTokenTableDef(tag: Tag) extends Table[RefreshTokenEntity](tag, "refresh_tokens") {

  def email = column[String]("EMAIL", O.PrimaryKey)

  def refreshToken = column[String]("REFRESH_TOKEN")

  def empId = column[Long]("EMP_ID")

  def createdAt = column[LocalDateTime]("CREATED_AT")

  override def * =
    (refreshToken, empId, email, createdAt).mapTo[RefreshTokenEntity]
}

object RefreshTokenTableDef {
  lazy val refreshTokens = TableQuery[RefreshTokenTableDef]
}
