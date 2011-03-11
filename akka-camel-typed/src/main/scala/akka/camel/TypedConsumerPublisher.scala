/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.camel

import java.lang.reflect.Method

import akka.actor._
import akka.camel.component.TypedActorComponent

/**
 * @author Martin Krasser
 */
private[camel] class TypedConsumerPublishRequestor extends PublishRequestor {
  def receiveActorRegistryEvent = {
    case ActorRegistered(actor)    => for (event <- ConsumerMethodRegistered.eventsFor(actor)) deliverCurrentEvent(event)
    case ActorUnregistered(actor)  => for (event <- ConsumerMethodUnregistered.eventsFor(actor)) deliverCurrentEvent(event)
  }
}

/**
 * @author Martin Krasser
 */
private[camel] object TypedConsumerPublisher {
  /**
   * Creates a route to an typed actor method.
   */
  def handleConsumerMethodRegistered(event: ConsumerMethodRegistered) {
    CamelContextManager.mandatoryContext.addRoutes(new ConsumerMethodRouteBuilder(event))
    EventHandler notifyListeners EventHandler.Info(this, "published method %s of %s at endpoint %s" format (event.methodName, event.typedActor, event.endpointUri))
  }

  /**
   * Stops the route to the already un-registered consumer actor method.
   */
  def handleConsumerMethodUnregistered(event: ConsumerMethodUnregistered) {
    CamelContextManager.mandatoryContext.stopRoute(event.methodUuid)
    EventHandler notifyListeners EventHandler.Info(this, "unpublished method %s of %s from endpoint %s" format (event.methodName, event.typedActor, event.endpointUri))
  }
}

private[camel] class TypedConsumerPublisher(activationTracker: ActorRef) extends Actor {
  import TypedConsumerPublisher._

  def receive = {
    case mr: ConsumerMethodRegistered => {
      handleConsumerMethodRegistered(mr)
      activationTracker ! EndpointActivated
    }
    case mu: ConsumerMethodUnregistered => {
      handleConsumerMethodUnregistered(mu)
      activationTracker ! EndpointDeactivated
    }
    case _ => { /* ignore */}
  }
}

/**
 * Defines the route to a typed actor method.
 *
 * @author Martin Krasser
 */
private[camel] class ConsumerMethodRouteBuilder(event: ConsumerMethodRegistered) extends ConsumerRouteBuilder(event.endpointUri, event.methodUuid) {
  protected def routeDefinitionHandler: RouteDefinitionHandler = event.routeDefinitionHandler
  protected def targetUri = "%s:%s?method=%s" format (TypedActorComponent.InternalSchema, event.methodUuid, event.methodName)
}

/**
 * A consumer method (un)registration event.
 */
private[camel] trait ConsumerMethodEvent extends ConsumerEvent {
  val actorRef: ActorRef
  val method: Method

  val uuid = actorRef.uuid.toString
  val methodName = method.getName
  val methodUuid = "%s_%s" format (uuid, methodName)
  val typedActor = actorRef.actor.asInstanceOf[TypedActor].proxy

  lazy val routeDefinitionHandler = consumeAnnotation.routeDefinitionHandler.newInstance
  lazy val consumeAnnotation = method.getAnnotation(classOf[consume])
  lazy val endpointUri = consumeAnnotation.value
}

/**
 * Event indicating that a typed actor has been registered at the actor registry. For
 * each <code>@consume</code> annotated typed actor method a separate instance of this
 * class is created.
 */
private[camel] case class ConsumerMethodRegistered(actorRef: ActorRef, method: Method) extends ConsumerMethodEvent

/**
 * Event indicating that a typed actor has been unregistered from the actor registry. For
 * each <code>@consume</code> annotated typed actor method a separate instance of this
 * class is created.
 */
private[camel] case class ConsumerMethodUnregistered(actorRef: ActorRef, method: Method) extends ConsumerMethodEvent

/**
 * @author Martin Krasser
 */
private[camel] object ConsumerMethodRegistered {
  /**
   * Creates a list of ConsumerMethodRegistered event messages for a typed actor or an empty
   * list if the typed actor is a proxy for a remote typed actor or the typed actor doesn't
   * have any <code>@consume</code> annotated methods.
   */
  def eventsFor(actorRef: ActorRef): List[ConsumerMethodRegistered] = {
    TypedConsumer.withTypedConsumer(actorRef: ActorRef) {
      m => ConsumerMethodRegistered(actorRef, m)
    }
  }
}

/**
 * @author Martin Krasser
 */
private[camel] object ConsumerMethodUnregistered {
  /**
   * Creates a list of ConsumerMethodUnregistered event messages for a typed actor or an empty
   * list if the typed actor is a proxy for a remote typed actor or the typed actor doesn't
   * have any <code>@consume</code> annotated methods.
   */
  def eventsFor(actorRef: ActorRef): List[ConsumerMethodUnregistered] = {
    TypedConsumer.withTypedConsumer(actorRef) {
      m => ConsumerMethodUnregistered(actorRef, m)
    }
  }
}
