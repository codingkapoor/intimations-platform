package com.codingkapoor.employee.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.codingkapoor.employee.api.model.{Employee, EmployeeKafkaEvent, IntimationReq, IntimationRes, Leaves}

object EmployeeService {
  val TOPIC_NAME = "employee"
}

trait EmployeeService extends Service {

  def addEmployee(): ServiceCall[Employee, Done]

  def terminateEmployee(id: String): ServiceCall[NotUsed, Done]

  def getEmployees: ServiceCall[NotUsed, Seq[Employee]]

  def getEmployee(id: String): ServiceCall[NotUsed, Employee]

  def deleteEmployee(id: String): ServiceCall[NotUsed, Done]

  def getLeaves(empId: String): ServiceCall[NotUsed, Leaves]

  def createIntimation(empId: String): ServiceCall[IntimationReq, Done]

  def getIntimation(empId: String, month: Option[Int], year: Option[Int]): ServiceCall[NotUsed, IntimationRes]

  def getActiveIntimationsOfAllEmployees: ServiceCall[NotUsed, List[IntimationRes]]

  def employeeTopic: Topic[EmployeeKafkaEvent]

  override final def descriptor: Descriptor = {
    import Service._

    named("employee")
      .withCalls(
        restCall(Method.POST, "/api/employees", addEmployee _),
        restCall(Method.PUT, "/api/employees/:id/terminate", terminateEmployee _),
        restCall(Method.GET, "/api/employees", getEmployees _),
        restCall(Method.GET, "/api/employees/:id", getEmployee _),
        restCall(Method.DELETE, "/api/employees/:id", deleteEmployee _),
        restCall(Method.GET, "/api/employees/:id/leaves", getLeaves _),
        restCall(Method.POST, "/api/employees/:id/intimations", createIntimation _),
        restCall(Method.GET, "/api/employees/:id/intimations?month&year", getIntimation _),
        restCall(Method.GET, "/api/employees/intimations", getActiveIntimationsOfAllEmployees _)
      )
      .withTopics(
        topic(EmployeeService.TOPIC_NAME, employeeTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[EmployeeKafkaEvent](_.id)
          ))
      .withAutoAcl(true)
  }
}
