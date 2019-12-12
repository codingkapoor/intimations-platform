package com.codingkapoor.audit.service

import akka.Done
import akka.stream.scaladsl.Flow
import com.codingkapoor.employee.api.EmployeeService
import com.codingkapoor.employee.api.model.EmployeeKafkaEvent
import com.lightbend.lagom.scaladsl.api.Service.named
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import org.slf4j.LoggerFactory

trait AuditService extends Service {
  override final def descriptor: Descriptor = named("audit")
}

class AuditServiceImpl(employeeService: EmployeeService) extends AuditService {
  private val log = LoggerFactory.getLogger(classOf[AuditServiceImpl])

  employeeService
    .employeeTopic
    .subscribe
    .atLeastOnce(
      Flow[EmployeeKafkaEvent].map { ke =>
        log.info(s"$ke")
        Done
      }
    )
}
