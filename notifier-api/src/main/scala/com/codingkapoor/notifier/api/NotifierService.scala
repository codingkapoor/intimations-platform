package com.codingkapoor.notifier.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method

trait NotifierService extends Service {

  type ExpoToken = String

  def subscribe(empId: Long): ServiceCall[ExpoToken, Done]

  def unsubscribe(empId: Long): ServiceCall[NotUsed, Done]

  override def descriptor: Descriptor = {
    import Service._

    named("notifier")
      .withCalls(
        restCall(Method.PUT, "/api/notifier/employees/:empId/subscribe", subscribe _),
        restCall(Method.PUT, "/api/notifier/employees/:empId/unsubscribe", unsubscribe _),
      )
      .withAutoAcl(true)
  }
}
