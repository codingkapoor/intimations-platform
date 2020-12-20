package com.iamsmkr.employee.api

import java.time.LocalDate

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.iamsmkr.employee.api.models.{ActiveIntimation, ReleaseDate, Employee, EmployeeInfo, EmployeeKafkaEvent, InactiveIntimation, IntimationReq, Leaves, PrivilegedIntimation}

object EmployeeService {
  val TOPIC_NAME = "employee"
}

trait EmployeeService extends Service with EmployeePathParamSerializer {

  def addEmployee(): ServiceCall[Employee, Done]

  def updateEmployee(id: Long): ServiceCall[EmployeeInfo, Employee]

  def releaseEmployee(id: Long): ServiceCall[ReleaseDate, Done]

  def getEmployees(email: Option[String]): ServiceCall[NotUsed, Seq[Employee]]

  def getEmployee(id: Long): ServiceCall[NotUsed, Employee]

  def deleteEmployee(id: Long): ServiceCall[NotUsed, Done]

  def createIntimation(empId: Long): ServiceCall[IntimationReq, Leaves]

  def updateIntimation(empId: Long): ServiceCall[IntimationReq, Leaves]

  def cancelIntimation(empId: Long): ServiceCall[NotUsed, Leaves]

  def createPrivilegedIntimation(empId: Long): ServiceCall[PrivilegedIntimation, Leaves]

  def updatePrivilegedIntimation(empId: Long): ServiceCall[PrivilegedIntimation, Leaves]

  def cancelPrivilegedIntimation(empId: Long): ServiceCall[NotUsed, Leaves]

  def getInactiveIntimations(empId: Long, start: LocalDate, end: LocalDate): ServiceCall[NotUsed, List[InactiveIntimation]]

  def getActiveIntimations: ServiceCall[NotUsed, List[ActiveIntimation]]

  def employeeTopic: Topic[EmployeeKafkaEvent]

  override final def descriptor: Descriptor = {
    import Service._

    named("employee")
      .withCalls(
        restCall(Method.POST, "/api/employees", addEmployee _),
        restCall(Method.GET, "/api/employees/intimations", getActiveIntimations _),
        restCall(Method.PUT, "/api/employees/:id", updateEmployee _),
        restCall(Method.GET, "/api/employees?email", getEmployees _),
        restCall(Method.PUT, "/api/employees/:id/release", releaseEmployee _),
        restCall(Method.GET, "/api/employees/:id", getEmployee _),
        restCall(Method.DELETE, "/api/employees/:id", deleteEmployee _),
        restCall(Method.POST, "/api/employees/:id/intimations", createIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations", updateIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations/cancel", cancelIntimation _),
        restCall(Method.POST, "/api/employees/:id/intimations/privileged", createPrivilegedIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations/privileged", updatePrivilegedIntimation _),
        restCall(Method.PUT, "/api/employees/:id/intimations/privileged/cancel", cancelPrivilegedIntimation _),
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
