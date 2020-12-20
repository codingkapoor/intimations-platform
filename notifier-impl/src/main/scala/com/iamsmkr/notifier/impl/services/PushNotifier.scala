package com.iamsmkr.notifier.impl.services

import java.time.LocalDate

import play.api.libs.json.Json
import com.kinoroy.expo.push._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import java.util

import com.iamsmkr.employee.api.models.RequestType
import com.iamsmkr.notifier.api.models.IntimationType._
import com.iamsmkr.notifier.api.models.Notification
import com.iamsmkr.notifier.impl.repositories.employee.EmployeeDao

class PushNotifier(employeeDao: EmployeeDao) {

  import PushNotifier._

  val logger: Logger = LoggerFactory.getLogger(classOf[PushNotifier])

  def sendNotification(notification: Notification): Future[Unit] = {

    employeeDao.getEmployees.map { employees =>
      val messages = mutable.ListBuffer[Message]()
      val employeesWithTokens = employees.filterNot(e => e.empId == notification.empId).filter(_.expoToken.isDefined)

      for (e <- employeesWithTokens) {
        val token = e.expoToken.get

        if (!ExpoPushClient.isExpoPushToken(token)) {
          employeeDao.updateEmployee(e.copy(expoToken = None))
          logger.warn(s"Deleted an invalid expo token = ${token} against empId = ${e.empId}")
        } else {
          val (title, body, data) = getContent(notification)

          val message =
            new Message.Builder()
              .to(token)
              .sound("default")
              .title(title)
              .body(body)
              .data(data)
              .channelId("push-notifications")
              .build()

          messages += message
        }
      }

      val chunks = ExpoPushClient.chunkItems(messages.asJava).asScala
      for (chunk <- chunks) {
        val res = ExpoPushClient.sendPushNotifications(chunk)
        if (res.getErrors != null)
          logger.error(s"${res.getErrors}")
      }

    } recover {
      case NonFatal(e) =>
        logger.error(s"${e.getLocalizedMessage}")
        e.printStackTrace()
    }
  }
}

object PushNotifier {

  import com.iamsmkr.notifier.impl.services.NotificationTemplates._

  private final val TODAY = "today"
  private final val TOMORROW = "tomorrow"
  private final val EMP_ID = "empId"
  private final val EMP_NAME = "empName"
  private final val REASON = "reason"
  private final val LAST_MODIFIED = "lastModified"
  private final val REQUEST = "requests"
  private final val TYPE = "type"

  private def getContent(notification: Notification): (String, String, util.HashMap[String, Object]) = {

    def getBody: String = {
      val firstRequest = notification.requests.toList.sortWith((r1, r2) => r1.date.isBefore(r2.date)).head
      val today = LocalDate.now()
      val tomorrow = LocalDate.parse(s"${today.getYear}-${"%02d".format(today.getMonthValue)}-${"%02d".format(today.getDayOfMonth + 1)}")

      val body = if (firstRequest.date.isEqual(today) || firstRequest.date.isEqual(tomorrow)) {
        val day = if (firstRequest.date.isEqual(today)) TODAY else TOMORROW

        if (firstRequest.firstHalf == RequestType.WFH && firstRequest.secondHalf == RequestType.WFO)
          WFH_WFO_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.WFO && firstRequest.secondHalf == RequestType.WFH)
          WFO_WFH_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.WFO && firstRequest.secondHalf == RequestType.Leave)
          WFO_LEAVE_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.Leave && firstRequest.secondHalf == RequestType.WFO)
          LEAVE_WFO_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.WFH && firstRequest.secondHalf == RequestType.Leave)
          WFH_LEAVE_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.Leave && firstRequest.secondHalf == RequestType.WFH)
          LEAVE_WFH_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.WFH && firstRequest.secondHalf == RequestType.WFH)
          WFH_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else if (firstRequest.firstHalf == RequestType.Leave && firstRequest.secondHalf == RequestType.Leave)
          LEAVE_PUSH_NOTIFICATION_TEMPLATE.format(day)
        else ""

      } else
        PLANNED_PUSH_NOTIFICATION_TEMPLATE

      body
    }

    val (title, body) = notification.intimationType match {
      case Created => (INTIMATION_CREATION_TITLE_TEMPLATE.format(notification.empName), getBody)
      case Updated => (INTIMATION_UPDATE_TITLE_TEMPLATE.format(notification.empName), getBody)
      case Cancelled => (INTIMATION_CANCELLATION_TITLE_TEMPLATE.format(notification.empName), "")
    }

    val data = new util.HashMap[String, Object]()
    data.put(EMP_ID, notification.empId.asInstanceOf[AnyRef])
    data.put(EMP_NAME, notification.empName.asInstanceOf[AnyRef])
    data.put(REASON, notification.reason.asInstanceOf[AnyRef])
    data.put(LAST_MODIFIED, notification.lastModified.toString.asInstanceOf[AnyRef])
    data.put(REQUEST, Json.stringify(Json.toJson(notification.requests)).asInstanceOf[AnyRef])
    data.put(TYPE, notification.intimationType.toString.asInstanceOf[AnyRef])

    (title, body, data)
  }
}
