package com.codingkapoor.notifier.impl.service

import com.codingkapoor.employee.api.model.RequestType
import com.codingkapoor.notifier.api.model.IntimationType.{Cancelled, Created, Updated}
import com.codingkapoor.notifier.api.model.Notification
import courier.Defaults._
import courier._
import javax.mail.internet.InternetAddress
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import scala.util.{Failure, Success}

class MailNotifier(config: Configuration) {

  import MailNotifier._

  val mail: Mail = getMailConfig(config)

  val mailer: Mailer =
    Mailer(mail.smtp.interface, mail.smtp.port)
      .auth(true)
      .as(mail.sender, mail.password)
      .startTls(true)()

  def sendNotification(notification: Notification): Unit = {

    val receiversOpt: Option[String] = config.getOptional[String]("mail.receivers")

    if (receiversOpt.isDefined) {
      val receivers: Seq[InternetAddress] = receiversOpt.get.split(",").toSeq.map(mailid => mailid.addr)

      val (subject, body) = getContent(notification)

      val envelope = Envelope(from = mail.sender.addr, _to = receivers, _subject = Some((subject, None)), _content = Text(body))

      mailer(envelope).onComplete {
        case Success(_) => logger.info("Mail notifications sent successfully.")
        case Failure(e) => e.printStackTrace()
      }
    } else logger.warn("No receivers configured for mail notifications!")

  }
}

object MailNotifier {

  import com.codingkapoor.notifier.impl.service.NotificationTemplates._

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

  case class SMTP(interface: String, port: Int)

  case class Mail(sender: String, password: String, smtp: SMTP)

  def getMailConfig(config: Configuration): Mail = {
    val interface: Option[String] = config.getOptional[String]("mail.smtp.interface")
    val port: Option[Int] = config.getOptional[Int]("mail.smtp.port")

    val email: Option[String] = config.getOptional[String]("mail.email")
    val password: Option[String] = config.getOptional[String]("mail.password")

    if (interface.isEmpty || port.isEmpty || email.isEmpty || password.isEmpty)
      logger.warn("Mail configurations missing. Resorting to default configurations.")

    Mail(
      email.getOrElse("intimations@glassbeam.com"),
      password.getOrElse("password"),
      SMTP(
        interface.getOrElse("mymail.myoutlookonline.com"),
        port.getOrElse(587)
      )
    )
  }

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
