package com.codingkapoor.simplelms.impl

import com.codingkapoor.simplelms.api
import com.codingkapoor.simplelms.api.SimplelmsService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

/**
  * Implementation of the SimplelmsService.
  */
class SimplelmsServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends SimplelmsService {

  override def hello(id: String) = ServiceCall { _ =>
    // Look up the simple-lms entity for the given ID.
    val ref = persistentEntityRegistry.refFor[SimplelmsEntity](id)

    // Ask the entity the Hello command.
    ref.ask(Hello(id))
  }

  override def useGreeting(id: String) = ServiceCall { request =>
    // Look up the simple-lms entity for the given ID.
    val ref = persistentEntityRegistry.refFor[SimplelmsEntity](id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseGreetingMessage(request.message))
  }


  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(SimplelmsEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[SimplelmsEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }
}
