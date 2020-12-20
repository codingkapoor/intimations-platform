package com.iamsmkr.notifier.impl.services

import akka.Done
import akka.stream.scaladsl.Flow
import com.iamsmkr.employee.api.EmployeeService
import com.iamsmkr.employee.api.models.{EmployeeAddedKafkaEvent, EmployeeDeletedKafkaEvent, EmployeeKafkaEvent, EmployeeReleasedKafkaEvent, EmployeeUpdatedKafkaEvent, IntimationCancelledKafkaEvent, IntimationCreatedKafkaEvent, IntimationUpdatedKafkaEvent}
import com.iamsmkr.notifier.api.models.{IntimationType, Notification}
import com.iamsmkr.notifier.impl.repositories.employee.{EmployeeDao, EmployeeEntity}
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global

trait EmployeeKafkaEventHandler {
  def employeeService: EmployeeService

  def employeeDao: EmployeeDao

  def mailNotifier: MailNotifier

  def pushNotifier: PushNotifier

  def logger: Logger

  employeeService
    .employeeTopic
    .subscribe
    .atLeastOnce(
      Flow[EmployeeKafkaEvent].map { ke =>
        ke match {
          case added: EmployeeAddedKafkaEvent =>
            employeeDao.addEmployee(EmployeeEntity(added.id, added.name, None))

          case released: EmployeeReleasedKafkaEvent =>
            employeeDao.deleteEmployee(released.id)

          case deleted: EmployeeDeletedKafkaEvent =>
            employeeDao.deleteEmployee(deleted.id)

          case created: IntimationCreatedKafkaEvent =>
            employeeDao.getEmployee(created.id).map { employeeOpt =>
              if (employeeOpt.isDefined) {
                val notification = Notification(created.id, employeeOpt.get.empName, created.lastModified, created.reason, created.requests, IntimationType.Created)

                mailNotifier.sendNotification(notification)
                pushNotifier.sendNotification(notification)
              }
            }

          case updated: IntimationUpdatedKafkaEvent =>
            employeeDao.getEmployee(updated.id).map { employeeOpt =>
              if (employeeOpt.isDefined) {
                val notification = Notification(updated.id, employeeOpt.get.empName, updated.lastModified, updated.reason, updated.requests, IntimationType.Updated)

                mailNotifier.sendNotification(notification)
                pushNotifier.sendNotification(notification)
              }
            }

          case cancelled: IntimationCancelledKafkaEvent =>
            employeeDao.getEmployee(cancelled.id).map { employeeOpt =>
              if (employeeOpt.isDefined) {
                val notification = Notification(cancelled.id, employeeOpt.get.empName, cancelled.lastModified, cancelled.reason, cancelled.requests, IntimationType.Cancelled)

                mailNotifier.sendNotification(notification)
                pushNotifier.sendNotification(notification)
              }
            }

          case _ =>
        }

        Done
      }
    )
}
