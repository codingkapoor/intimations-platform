package com.codingkapoor.employee.impl.util

import com.codingkapoor.employee.api.model.Role
import com.codingkapoor.employee.impl.persistence.read.repository.employee.EmployeeDao
import com.lightbend.lagom.scaladsl.api.transport.Forbidden
import org.pac4j.core.profile.CommonProfile
import org.slf4j.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait AuthValidator {
  def employeeDao: EmployeeDao

  def logger: Logger

  def validateTokenType(profile: CommonProfile): Unit = {
    if (profile.getAttribute("type") == "Refresh")
      throw Forbidden("Access token expected")
  }

  def validateIfProfileBelongsToAdmin(profile: CommonProfile): Unit = {
    val isAdmin = Await.result(employeeDao.getEmployee(profile.getId.toLong).map { e =>
      if (e.isDefined) e.get.roles.contains(Role.Admin)
      else {
        logger.error("No employee found to whom the access token supposedly belongs to")
        throw Forbidden("Authorization failed")
      }
    }, 5.seconds)

    if (!isAdmin) {
      logger.error("Admin privileges required")
      throw Forbidden("Authorization failed")
    }
  }

  def validateIfProfileBelongsToIndividualEmployee(profile: CommonProfile, empId: Long): Unit = {
    val isEmployee = Await.result(employeeDao.getEmployee(profile.getId.toLong).map { e =>
      if (e.isDefined) e.get.roles.contains(Role.Employee)
      else {
        logger.error("No employee found to whom the access token supposedly belongs to")
        throw Forbidden("Authorization failed")
      }
    }, 5.seconds)

    if (profile.getId != empId.toString || !isEmployee) {
      logger.error("Employees can access their own data only provided they have employee privileges")
      throw Forbidden("Authorization failed")
    }
  }

  def validateIfProfileBelongsToIndividualEmployeeOrAdmin(profile: CommonProfile, empId: Long): Unit = {
    val isAdmin = Await.result(employeeDao.getEmployee(profile.getId.toLong).map { e =>
      if (e.isDefined) e.get.roles.contains(Role.Admin)
      else {
        logger.error("No employee found to whom the provided access token supposedly belongs to")
        throw Forbidden("Authorization failed")
      }
    }, 5.seconds)

    if (profile.getId != empId.toString && !isAdmin) {
      logger.error("Employees can access their own data only unless they have admin role")
      throw Forbidden("Authorization failed")
    }
  }

  def validateIfProfileBelongsToAnyEmployeeOrAdmin(profile: CommonProfile): Unit = {
    val isEmployeeOrAdmin = Await.result(employeeDao.getEmployee(profile.getId.toLong).map { e =>
      if (e.isDefined) e.get.roles.contains(Role.Admin) || e.get.roles.contains(Role.Employee)
      else {
        logger.error("No employee found to whom the provided access token supposedly belongs to")
        throw Forbidden("Authorization failed")
      }
    }, 5.seconds)

    if (!isEmployeeOrAdmin) {
      logger.error("Admin/Employee privilege required")
      throw Forbidden("Authorization failed")
    }
  }

}
