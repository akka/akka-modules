/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.camel

import org.apache.camel.CamelContext

import akka.actor.Actor._
import akka.actor.ActorRef
import akka.camel.component.TypedActorComponent

/**
 * @author Martin Krasser
 */
private[camel] object TypedCamel {
  private var consumerPublisher: ActorRef = _
  private var publishRequestor: ActorRef = _

  def onCamelContextInit(context: CamelContext) {
    context.addComponent(TypedActorComponent.InternalSchema, new TypedActorComponent)
  }

  def onCamelServiceStart(service: CamelService) {
    consumerPublisher = actorOf(new TypedConsumerPublisher(service.activationTracker))
    publishRequestor = actorOf(new TypedConsumerPublishRequestor)

    registerPublishRequestor

    for (event <- PublishRequestor.pastActorRegisteredEvents) publishRequestor ! event
    publishRequestor ! InitPublishRequestor(consumerPublisher.start)
  }

  def onCamelServiceStop(service: CamelService) {
    unregisterPublishRequestor
    consumerPublisher.stop
  }

  private def registerPublishRequestor: Unit = registry.addListener(publishRequestor)
  private def unregisterPublishRequestor: Unit = registry.removeListener(publishRequestor)
}