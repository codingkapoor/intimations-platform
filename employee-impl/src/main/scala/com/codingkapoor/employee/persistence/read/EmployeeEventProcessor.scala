package com.codingkapoor.employee.persistence.read

import akka.Done
import com.codingkapoor.employee.persistence.read.dao.employee.{EmployeeEntity, EmployeeRepository}
import com.codingkapoor.employee.persistence.read.dao.intimation.{IntimationEntity, IntimationRepository}
import com.codingkapoor.employee.persistence.read.dao.request.{RequestEntity, RequestRepository}
import com.codingkapoor.employee.persistence.write.{EmployeeAdded, EmployeeDeleted, EmployeeEvent, EmployeeTerminated, IntimationCancelled, IntimationCreated, IntimationUpdated}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

class EmployeeEventProcessor(readSide: SlickReadSide, employeeRepository: EmployeeRepository,
                             intimationRepository: IntimationRepository, requestRepository: RequestRepository) extends ReadSideProcessor[EmployeeEvent] {
  private val log = LoggerFactory.getLogger(classOf[EmployeeEventProcessor])

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[EmployeeEvent] =
    readSide
      .builder[EmployeeEvent]("employeeoffset")
      .setGlobalPrepare(employeeRepository.createTable andThen intimationRepository.createTable andThen requestRepository.createTable)
      .setEventHandler[EmployeeAdded](processEmployeeAdded)
      .setEventHandler[EmployeeTerminated](processEmployeeTerminated)
      .setEventHandler[EmployeeDeleted](processEmployeeDeleted)
      .setEventHandler[IntimationCreated](processIntimationCreated)
      //      .setEventHandler[IntimationUpdated](processIntimationUpdated)
      //      .setEventHandler[IntimationCancelled](processIntimationCancelled)
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

  private def processIntimationCreated(eventStreamElement: EventStreamElement[IntimationCreated]): DBIO[List[Done]] = {
    log.info(s"EmployeeEventProcessor received IntimationCreated event.")

    val created = eventStreamElement.event
    val empId = created.empId
    val reason = created.reason
    val requests = created.requests.toList.sortWith { case (rd1, rd2) => rd1.date.isBefore(rd2.date) }

    val latestRequestDate = requests.last.date

    val ie = IntimationEntity(None, empId, reason, latestRequestDate)

    intimationRepository.createIntimation(ie).flatMap { id =>
      DBIO.sequence(requests.map { request =>
        val date = request.date.getDayOfMonth
        val month = request.date.getMonthValue
        val year = request.date.getYear

        val re = RequestEntity(None, date, month, year, request.requestType, id)
        log.debug(s"RequestEntity = $re")

        requestRepository.addRequest(re)
      })
    }
  }

}
