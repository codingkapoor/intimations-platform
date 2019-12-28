package com.codingkapoor.passwordless.impl.service

import java.time.LocalDateTime
import courier.Defaults._
import courier._
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import scala.util.{Failure, Success}

class MailOTPService(config: Configuration) {

  import MailOTPService._

  val mail: Mail = getMailConfig(config)

  def sendOTP(receiver: String, otp: Int): Unit = {
    val mailer =
      Mailer(mail.smtp.interface, mail.smtp.port)
        .auth(true)
        .as(mail.sender, mail.password)
        .startTls(true)()

    // TODO: Expiry string has to be made readable
    val envelope =
      Envelope.from(mail.sender.addr)
        .to(receiver.addr)
        .subject(SUBJECT.format(otp))
        .content(Text(BODY.format(otp, LocalDateTime.now())))

    mailer(envelope).onComplete {
      case Success(_) => log.info("OTP sent successfully.")
      case Failure(e) => e.printStackTrace()
    }
  }
}

object MailOTPService {
  val log: Logger = LoggerFactory.getLogger(classOf[MailOTPService])

  final val SUBJECT = "%s is your OTP for login"
  final val BODY =
    """
      |Hello,
      |
      |%s is your OTP for login at Intimations. This OTP is valid till %s.
      |
      |Please don't share your OTP with anyone for security reasons.
      |
      |Regards,
      |Team Intimations
      |""".stripMargin

  case class SMTP(interface: String, port: Int)

  case class Mail(sender: String, password: String, smtp: SMTP)

  def getMailConfig(config: Configuration): Mail = {
    val interface: Option[String] = config.getOptional[String]("mail.smtp.interface")
    val port: Option[Int] = config.getOptional[Int]("mail.smtp.port")

    val email: Option[String] = config.getOptional[String]("mail.email")
    val password: Option[String] = config.getOptional[String]("mail.password")

    if (interface.isEmpty || port.isEmpty || email.isEmpty || password.isEmpty)
      log.warn("Mail configurations missing. Resorting to default configurations.")

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
