package com.iamsmkr.common

import courier.Defaults._
import courier._
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import scala.concurrent.Future

class Mailer(config: Configuration) {

  import MailNotifier._

  val mail: Mail = getMailConfig(config)

  val mailer: courier.Mailer =
    Mailer(mail.smtp.interface, mail.smtp.port)
      .auth(true)
      .as(mail.sender, mail.password)
      .startTls(true)()

  def sendMail(receivers: Seq[String], subject: String, body: String): Future[Unit] = {

    mailer(
      Envelope(
        from = mail.sender.addr,
        _to = receivers.map(mailid => mailid.addr),
        _subject = Some((subject, None)),
        _content = Text(body)
      )
    )
  }
}

object MailNotifier {

  val logger: Logger = LoggerFactory.getLogger(classOf[Mailer])

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
}
