package com.codingkapoor.passwordless.impl.repositories.token

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future

// TODO: Check if the db connections are managed
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

  def deleteRefreshToken(email: String): Future[Int] = {
    db.run(refreshTokens.filter(_.email === email).delete)
  }

  private def createTable: Future[Unit] = db.run(refreshTokens.schema.createIfNotExists)
}
