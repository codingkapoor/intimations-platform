package com.iamsmkr.passwordless.impl.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.iamsmkr.common.Mailer
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class OTPMailer(config: Configuration, mailer: Mailer) {

  import OTPMailer._

  def sendOTP(receiver: String, otp: Int): Unit = {

    val expiriesAt = LocalDateTime.now().plusMinutes(config.getOptional[Long]("expiries.otp").getOrElse(5)).format(formatter)

    mailer.sendMail(Seq(receiver), SUBJECT.format(otp), BODY.format(otp, expiriesAt)).onComplete {
      case Success(_) => logger.info("OTP sent successfully.")
      case Failure(e) => e.printStackTrace()
    }
  }
}

object OTPMailer {
  val logger: Logger = LoggerFactory.getLogger(classOf[OTPMailer])

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
}
