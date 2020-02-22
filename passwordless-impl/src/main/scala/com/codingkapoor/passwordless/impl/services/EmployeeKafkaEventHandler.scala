package com.codingkapoor.passwordless.impl.services

import akka.Done
import akka.stream.scaladsl.Flow
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.models.{EmployeeAddedKafkaEvent, EmployeeDeletedKafkaEvent, EmployeeKafkaEvent, EmployeeReleasedKafkaEvent, EmployeeUpdatedKafkaEvent}
import com.codingkapoor.passwordless.impl.repositories.employee.{EmployeeDao, EmployeeEntity}
import com.codingkapoor.passwordless.impl.repositories.otp.OTPDao
import com.codingkapoor.passwordless.impl.repositories.token.RefreshTokenDao
import org.slf4j.Logger
import scala.concurrent.ExecutionContext.Implicits.global

trait EmployeeKafkaEventHandler {
  def employeeService: EmployeeService

  def employeeDao: EmployeeDao

  def otpDao: OTPDao

  def refreshTokenDao: RefreshTokenDao

  def logger: Logger

  employeeService
    .employeeTopic
    .subscribe
    .atLeastOnce(
      Flow[EmployeeKafkaEvent].map { ke =>
        ke match {
          case added: EmployeeAddedKafkaEvent =>
            employeeDao.addEmployee(EmployeeEntity(added.id, added.name, added.contactInfo.email, added.roles))

          case updated: EmployeeUpdatedKafkaEvent =>
            employeeDao.updateEmployee(EmployeeEntity(updated.id, updated.name, updated.contactInfo.email, updated.roles))

          case released: EmployeeReleasedKafkaEvent =>
            val empId = released.id

            for {
              _ <- employeeDao.deleteEmployee(empId)
              _ <- otpDao.deleteOTP(empId)
              _ <- refreshTokenDao.deleteRefreshToken(empId)
            } yield {
              logger.info(s"EmployeeReleasedKafkaEvent kafka event received. Deleted both OTPs and Refresh Tokens that belonged to empId = ${ke.id}, if any.")
            }

          case deleted: EmployeeDeletedKafkaEvent =>
            val empId = deleted.id

            for {
              _ <- employeeDao.deleteEmployee(empId)
              _ <- otpDao.deleteOTP(empId)
              _ <- refreshTokenDao.deleteRefreshToken(empId)
            } yield {
              logger.info(s"EmployeeDeletedKafkaEvent kafka event received. Deleted both OTPs and Refresh Tokens that belonged to empId = ${ke.id}, if any.")
            }

          case _ =>
        }

        Done
      }
    )
}
