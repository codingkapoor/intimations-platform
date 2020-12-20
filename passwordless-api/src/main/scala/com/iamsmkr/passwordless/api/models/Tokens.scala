package com.iamsmkr.passwordless.api.models

import play.api.libs.json.{Format, Json}

case class Tokens(access: String, refresh: String)

object Tokens {
  implicit val format: Format[Tokens] = Json.format[Tokens]
}
