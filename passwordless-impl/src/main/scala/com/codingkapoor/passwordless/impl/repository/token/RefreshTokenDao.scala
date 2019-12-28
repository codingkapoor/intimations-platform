package com.codingkapoor.passwordless.impl.repository.token

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future

class RefreshTokenDao(db: Database) {

  val refreshTokens = RefreshTokenTableDef.refreshTokens

  createTable

  def addRefreshToken(refreshToken: RefreshTokenEntity): Future[Int] = {
    db.run(refreshTokens += refreshToken)
  }

  def getRefreshToken(email: String): Future[Option[RefreshTokenEntity]] = {
    db.run(refreshTokens.filter(_.email === email).result.headOption)
  }

  def deleteRefreshToken(empId: Long): Future[Int] = {
    db.run(refreshTokens.filter(_.empId === empId).delete)
  }

  private def createTable: Future[Unit] = db.run(refreshTokens.schema.createIfNotExists)
}
