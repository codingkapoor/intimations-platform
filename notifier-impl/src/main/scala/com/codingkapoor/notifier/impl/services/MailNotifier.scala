package com.codingkapoor.notifier.impl.services

import com.codingkapoor.common.Mailer
import com.codingkapoor.employee.api.models.RequestType
import com.codingkapoor.notifier.api.models.IntimationType.{Cancelled, Created, Updated}
import com.codingkapoor.notifier.api.models.Notification
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class MailNotifier(config: Configuration, mailer: Mailer) {

  import MailNotifier._

  def sendNotification(notification: Notification): Unit = {

    val receiversOpt: Option[String] = config.getOptional[String]("mail.receivers")

    if (receiversOpt.isDefined) {
      val receivers: Seq[String] = receiversOpt.get.split(",").toSeq
      val (subject, body) = getContent(notification)

      mailer.sendMail(receivers, subject, body).onComplete {
        case Success(_) => logger.info("Mail notifications sent successfully.")
        case Failure(e) => e.printStackTrace()
      }
    } else logger.warn("No receivers configured for mail notifications!")

  }
}

object MailNotifier {

  import com.codingkapoor.notifier.impl.services.NotificationTemplates._

  val logger: Logger = LoggerFactory.getLogger(classOf[MailNotifier])

  private final val BODY =
    """
      |
      |Reason
      |----------------
      |%s
      |
      |
      |Requests
      |-----------------
      |%s
      |
      |""".stripMargin

  private def getContent(notification: Notification): (String, String) = {

    def getBody: String = {
      val requests = StringBuilder.newBuilder
      for (request <- notification.requests.toList.sortWith((r1, r2) => r1.date.isBefore(r2.date))) {
        val date = request.date.toString

        if (request.firstHalf == RequestType.WFH && request.secondHalf == RequestType.WFO)
          requests.append(s"${WFH_WFO_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.WFO && request.secondHalf == RequestType.WFH)
          requests.append(s"${WFO_WFH_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.WFO && request.secondHalf == RequestType.Leave)
          requests.append(s"${WFO_LEAVE_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.Leave && request.secondHalf == RequestType.WFO)
          requests.append(s"${LEAVE_WFO_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.WFH && request.secondHalf == RequestType.Leave)
          requests.append(s"${WFH_LEAVE_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.Leave && request.secondHalf == RequestType.WFH)
          requests.append(s"${LEAVE_WFH_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.WFH && request.secondHalf == RequestType.WFH)
          requests.append(s"${WFH_MAIL_BODY_TEMPLATE.format(date)}\n")
        else if (request.firstHalf == RequestType.Leave && request.secondHalf == RequestType.Leave)
          requests.append(s"${LEAVE_MAIL_BODY_TEMPLATE.format(date)}\n")
        else ""
      }

      BODY.format(notification.reason, requests)
    }

    val (subject, body) = notification.intimationType match {
      case Created => (INTIMATION_CREATION_TITLE_TEMPLATE.format(notification.empName), getBody)
      case Updated => (INTIMATION_UPDATE_TITLE_TEMPLATE.format(notification.empName), getBody)
      case Cancelled => (INTIMATION_CANCELLATION_TITLE_TEMPLATE.format(notification.empName), "")
    }

    (subject, body)
  }
}
