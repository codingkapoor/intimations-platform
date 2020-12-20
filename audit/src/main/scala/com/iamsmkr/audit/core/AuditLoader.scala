package com.iamsmkr.audit.core

import com.iamsmkr.audit.services.AuditService
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}

class AuditLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AuditApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AuditApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[AuditService])
}
