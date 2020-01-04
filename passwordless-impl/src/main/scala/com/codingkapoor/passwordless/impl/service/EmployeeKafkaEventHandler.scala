package com.codingkapoor.passwordless.impl.service

import akka.Done
import akka.stream.scaladsl.Flow
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.model.{EmployeeAddedKafkaEvent, EmployeeDeletedKafkaEvent, EmployeeKafkaEvent, EmployeeTerminatedKafkaEvent, EmployeeUpdatedKafkaEvent}
import com.codingkapoor.passwordless.impl.repository.employee.{EmployeeDao, EmployeeEntity}
import com.codingkapoor.passwordless.impl.repository.otp.OTPDao
import com.codingkapoor.passwordless.impl.repository.token.RefreshTokenDao
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
            employeeDao.addEmployee(EmployeeEntity(added.id, added.name, added.gender, added.doj, added.designation, added.pfn,
              added.isActive, added.contactInfo.phone, added.contactInfo.email, added.location.city, added.location.state,
              added.location.country, added.leaves.earned, added.leaves.sick, added.roles))

          case updated: EmployeeUpdatedKafkaEvent =>
            employeeDao.updateEmployee(EmployeeEntity(updated.id, updated.name, updated.gender, updated.doj, updated.designation, updated.pfn,
              updated.isActive, updated.contactInfo.phone, updated.contactInfo.email, updated.location.city, updated.location.state,
              updated.location.country, updated.leaves.earned, updated.leaves.sick, updated.roles))

          case terminated: EmployeeTerminatedKafkaEvent =>
            val empId = terminated.id

            for {
              _ <- employeeDao.terminateEmployee(empId)
              _ <- otpDao.deleteOTP(empId)
              _ <- refreshTokenDao.deleteRefreshToken(empId)
            } yield {
              logger.info(s"EmployeeTerminatedKafkaEvent kafka event received. Deleted both OTPs and Refresh Tokens that belonged to empId = ${ke.id}, if any.")
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
