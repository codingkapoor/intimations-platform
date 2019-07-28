package com.codingkapoor.employee.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Json, Writes}

trait EmployeeServiceApi extends Service {

  def addEmployee(): ServiceCall[Employee, Done]

  def updateEmployee(id: String): ServiceCall[Employee, Done]

  def getEmployee(id: String): ServiceCall[NotUsed, Employee]

  def getEmployees: ServiceCall[NotUsed, Seq[Employee]]

  override final def descriptor: Descriptor = {
    import Service._

    named("employee")
      .withCalls(
        restCall(Method.POST, "/api/employees", addEmployee _),
        restCall(Method.PUT, "/api/employees/:id", updateEmployee _),
        restCall(Method.GET, "/api/employees/:id", getEmployee _),
        restCall(Method.GET, "/api/employees", getEmployees _)
      )
      .withAutoAcl(true)
  }
}
