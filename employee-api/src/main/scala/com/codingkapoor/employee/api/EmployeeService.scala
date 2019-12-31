package com.codingkapoor.employee.api

import java.time.LocalDate
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

import com.codingkapoor.employee.api.model.{ActiveIntimation, Employee, EmployeeInfo, EmployeeKafkaEvent, InactiveIntimation, IntimationReq}

object EmployeeService {
  val TOPIC_NAME = "employee"
}

trait EmployeeService extends Service with EmployeePathParamSerializer {

  def addEmployee(): ServiceCall[Employee, Done]

  def updateEmployee(id: Long): ServiceCall[EmployeeInfo, Employee]

  def terminateEmployee(id: Long): ServiceCall[NotUsed, Done]

  def getEmployees(email: Option[String]): ServiceCall[NotUsed, Seq[Employee]]

  def getEmployee(id: Long): ServiceCall[NotUsed, Employee]

  def deleteEmployee(id: Long): ServiceCall[NotUsed, Done]

  def createIntimation(empId: Long): ServiceCall[IntimationReq, Done]

  def updateIntimation(empId: Long): ServiceCall[IntimationReq, Done]

  def cancelIntimation(empId: Long): ServiceCall[NotUsed, Done]

  def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): ServiceCall[NotUsed, List[InactiveIntimation]]

  def getActiveIntimations: ServiceCall[NotUsed, List[ActiveIntimation]]

  def employeeTopic: Topic[EmployeeKafkaEvent]

  override final def descriptor: Descriptor = {
    import Service._

    named("employee")
      .withCalls(
        restCall(Method.POST, "/api/employees", addEmployee _),
        restCall(Method.PUT, "/api/employees/:id", updateEmployee _),
        restCall(Method.GET, "/api/employees?email", getEmployees _),
        restCall(Method.GET, "/api/employees/intimations", getActiveIntimations _),
        restCall(Method.PUT, "/api/employees/:id/terminate", terminateEmployee _),
        restCall(Method.GET, "/api/employees/:id", getEmployee _),
        restCall(Method.DELETE, "/api/employees/:id", deleteEmployee _),
        restCall(Method.POST, "/api/employees/:id/intimations", createIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations", updateIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations/cancel", cancelIntimation _),
        restCall(Method.GET, "/api/employees/:id/intimations?start&end", getInactiveIntimations _)
      )
      .withTopics(
        topic(EmployeeService.TOPIC_NAME, employeeTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[EmployeeKafkaEvent](_.id.toString)
          ))
      .withAutoAcl(true)
  }
}
