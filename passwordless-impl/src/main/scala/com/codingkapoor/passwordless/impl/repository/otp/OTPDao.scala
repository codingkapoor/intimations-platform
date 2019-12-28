package com.codingkapoor.passwordless.impl.repository.otp

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.Future

class OTPDao(db: Database) {

  val otps = OTPTableDef.otps

  createTable

  def createOTP(otp: OTPEntity): Future[Int] = {
    db.run(otps += otp)
  }

  def getOTP(email: String): Future[Option[OTPEntity]] = {
    db.run(otps.filter(_.email === email).result.headOption)
  }

  def deleteOTP(otp: Int): Future[Int] = {
    db.run(otps.filter(_.otp === otp).delete)
  }

  private def createTable: Future[Unit] = db.run(otps.schema.createIfNotExists)
}
