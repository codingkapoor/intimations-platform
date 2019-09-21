package com.codingkapoor.employee.persistence.read

import akka.Done
import com.codingkapoor.employee.persistence.write.{EmployeeAdded, EmployeeDeleted, EmployeeEvent, EmployeeTerminated}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._

class EmployeeEventProcessor(readSide: SlickReadSide, employeeRepository: EmployeeRepository)
  extends ReadSideProcessor[EmployeeEvent] {
  private val log = LoggerFactory.getLogger(classOf[EmployeeEventProcessor])

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[EmployeeEvent] =
    readSide
      .builder[EmployeeEvent]("employeeoffset")
      .setGlobalPrepare(employeeRepository.createTable)
      .setEventHandler[EmployeeAdded](processEmployeeAdded)
      .setEventHandler[EmployeeTerminated](processEmployeeTerminated)
      .setEventHandler[EmployeeDeleted](processEmployeeDeleted)
      .build()

  override def aggregateTags: Set[AggregateEventTag[EmployeeEvent]] = Set(EmployeeEvent.Tag)

  private def processEmployeeAdded(eventStreamElement: EventStreamElement[EmployeeAdded]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeAdded event.")

    val employeeAdded = eventStreamElement.event
    val employee = EmployeeEntity(employeeAdded.id, employeeAdded.name, employeeAdded.gender, employeeAdded.doj, employeeAdded.pfn, employeeAdded.isActive)

    employeeRepository.addEmployee(employee)
  }

  private def processEmployeeTerminated(eventStreamElement: EventStreamElement[EmployeeTerminated]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeTerminated event.")

    val employeeTerminated = eventStreamElement.event
    val employee = EmployeeEntity(employeeTerminated.id, employeeTerminated.name, employeeTerminated.gender, employeeTerminated.doj, employeeTerminated.pfn, employeeTerminated.isActive)

    employeeRepository.terminateEmployee(employee)
  }

  private def processEmployeeDeleted(eventStreamElement: EventStreamElement[EmployeeDeleted]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeDeleted event.")

    val employeeDeleted = eventStreamElement.event
    employeeRepository.deleteEmployee(employeeDeleted.id)
  }

}
