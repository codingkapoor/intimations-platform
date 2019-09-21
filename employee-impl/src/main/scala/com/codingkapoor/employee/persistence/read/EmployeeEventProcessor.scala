package com.codingkapoor.employee.persistence.read

import akka.Done
import com.codingkapoor.employee.persistence.read.dao.employee.{EmployeeEntity, EmployeeRepository}
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

    val added = eventStreamElement.event

    val employee =
      EmployeeEntity(added.id, added.name, added.gender, added.doj, added.pfn, added.isActive, added.leaves.earned, added.leaves.sick)

    employeeRepository.addEmployee(employee)
  }

  private def processEmployeeTerminated(eventStreamElement: EventStreamElement[EmployeeTerminated]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeTerminated event.")

    val terminated = eventStreamElement.event

    val employee =
      EmployeeEntity(terminated.id, terminated.name, terminated.gender, terminated.doj, terminated.pfn,
        terminated.isActive, terminated.leaves.earned, terminated.leaves.sick)

    employeeRepository.terminateEmployee(employee)
  }

  private def processEmployeeDeleted(eventStreamElement: EventStreamElement[EmployeeDeleted]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeDeleted event.")

    val deleted = eventStreamElement.event
    employeeRepository.deleteEmployee(deleted.id)
  }

}
