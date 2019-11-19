package com.codingkapoor.employee.persistence.read

import akka.Done
import com.codingkapoor.employee.persistence.read.dao.employee.{EmployeeEntity, EmployeeRepository}
import com.codingkapoor.employee.persistence.read.dao.intimation.{IntimationEntity, IntimationRepository}
import com.codingkapoor.employee.persistence.read.dao.request.{RequestEntity, RequestRepository}
import com.codingkapoor.employee.persistence.write.{EmployeeAdded, EmployeeDeleted, EmployeeEvent, EmployeeTerminated, EmployeeUpdated, IntimationCancelled, IntimationCreated, IntimationUpdated}
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
      .setEventHandler[EmployeeUpdated](processEmployeeUpdated)
      .setEventHandler[EmployeeTerminated](processEmployeeTerminated)
      .setEventHandler[EmployeeDeleted](processEmployeeDeleted)
      .setEventHandler[IntimationCreated](processIntimationCreated)
      .setEventHandler[IntimationUpdated](processIntimationUpdated)
      .setEventHandler[IntimationCancelled](processIntimationCancelled)
      .build()

  override def aggregateTags: Set[AggregateEventTag[EmployeeEvent]] = Set(EmployeeEvent.Tag)

  private def processEmployeeAdded(eventStreamElement: EventStreamElement[EmployeeAdded]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeAdded event.")

    val added = eventStreamElement.event

    val employee =
      EmployeeEntity(added.id, added.name, added.gender, added.doj, added.designation, added.pfn, added.isActive, added.contactInfo.phone,
        added.contactInfo.email, added.location.city, added.location.state, added.location.country, added.leaves.earned, added.leaves.sick)

    employeeRepository.addEmployee(employee)
  }

  private def processEmployeeUpdated(eventStreamElement: EventStreamElement[EmployeeUpdated]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeUpdated event.")

    val updated = eventStreamElement.event

    val employee =
      EmployeeEntity(updated.id, updated.name, updated.gender, updated.doj, updated.designation, updated.pfn, updated.isActive, updated.contactInfo.phone,
        updated.contactInfo.email, updated.location.city, updated.location.state, updated.location.country, updated.leaves.earned, updated.leaves.sick)

    employeeRepository.updateEmployee(employee)
  }

  private def processEmployeeTerminated(eventStreamElement: EventStreamElement[EmployeeTerminated]): DBIO[Done] = {
    log.info(s"EmployeeEventProcessor received EmployeeTerminated event.")

    val terminated = eventStreamElement.event

    val employee =
      EmployeeEntity(terminated.id, terminated.name, terminated.gender, terminated.doj, terminated.designation, terminated.pfn,
        terminated.isActive, terminated.contactInfo.phone, terminated.contactInfo.email, terminated.location.city,
        terminated.location.state, terminated.location.country, terminated.leaves.earned, terminated.leaves.sick)

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
    val lastModified = created.lastModified

    val ie = IntimationEntity(empId, reason, latestRequestDate, lastModified)

    intimationRepository.createIntimation(ie).flatMap { id =>
      DBIO.sequence(requests.map { request =>
        val date = request.date.getDayOfMonth
        val month = request.date.getMonthValue
        val year = request.date.getYear

        val re = RequestEntity(date, month, year, request.firstHalf, request.secondHalf, id)
        log.debug(s"RequestEntity = $re")

        requestRepository.addRequest(re)
      })
    }
  }

  private def processIntimationUpdated(eventStreamElement: EventStreamElement[IntimationUpdated]): DBIO[List[Done]] = {
    log.info(s"EmployeeEventProcessor received IntimationUpdated event.")

    val updated = eventStreamElement.event
    val empId = updated.empId
    val reason = updated.reason
    val requests = updated.requests.toList.sortWith { case (rd1, rd2) => rd1.date.isBefore(rd2.date) }

    val latestRequestDate = requests.last.date
    val lastModified = updated.lastModified

    val ie = IntimationEntity(empId, reason, latestRequestDate, lastModified)

    intimationRepository.deleteIntimation(empId).flatMap { _ =>
      intimationRepository.createIntimation(ie).flatMap { id =>
        DBIO.sequence(requests.map { request =>
          val date = request.date.getDayOfMonth
          val month = request.date.getMonthValue
          val year = request.date.getYear

          val re = RequestEntity(date, month, year, request.firstHalf, request.secondHalf, id)
          log.debug(s"RequestEntity = $re")

          requestRepository.addRequest(re)
        })
      }
    }
  }

  private def processIntimationCancelled(eventStreamElement: EventStreamElement[IntimationCancelled]): DBIO[List[Done]] = {
    log.info(s"EmployeeEventProcessor received IntimationCancelled event.")

    val cancelled = eventStreamElement.event
    val empId = cancelled.empId
    val reason = cancelled.reason
    val requests = cancelled.requests.toList.sortWith { case (rd1, rd2) => rd1.date.isBefore(rd2.date) }
    val lastModified = cancelled.lastModified

    if (requests.nonEmpty) {
      val latestRequestDate = requests.last.date

      val ie = IntimationEntity(empId, reason, latestRequestDate, lastModified)

      intimationRepository.deleteIntimation(empId).flatMap { _ =>
        intimationRepository.createIntimation(ie).flatMap { id =>
          DBIO.sequence(requests.map { request =>
            val date = request.date.getDayOfMonth
            val month = request.date.getMonthValue
            val year = request.date.getYear

            val re = RequestEntity(date, month, year, request.firstHalf, request.secondHalf, id)
            log.debug(s"RequestEntity = $re")

            requestRepository.addRequest(re)
          })
        }
      }
    } else intimationRepository.deleteIntimation(empId).map(List(_))
  }

}
