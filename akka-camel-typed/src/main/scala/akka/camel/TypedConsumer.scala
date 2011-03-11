/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.camel

import java.lang.reflect.Method

import akka.actor.{TypedActor, ActorRef}

/**
 * @author Martin Krasser
 */
private[camel] object TypedConsumer {
  /**
   * Applies a function <code>f</code> to each consumer method of <code>TypedActor</code> and
   * returns the function results as a list. A consumer method is one that is annotated with
   * <code>@consume</code>. If <code>actorRef</code> is a remote actor reference, <code>f</code>
   * is never called and <code>Nil</code> is returned.
   */
  def withTypedConsumer[T](actorRef: ActorRef)(f: Method => T): List[T] = {
    if (!actorRef.actor.isInstanceOf[TypedActor]) Nil
    else if (actorRef.homeAddress.isDefined) Nil
    else {
      val typedActor = actorRef.actor.asInstanceOf[TypedActor]
      // TODO: support consumer annotation inheritance
      // - visit overridden methods in superclasses
      // - visit implemented method declarations in interfaces
      val intfClass = typedActor.proxy.getClass
      val implClass = typedActor.getClass
      (for (m <- intfClass.getMethods.toList; if (m.isAnnotationPresent(classOf[consume]))) yield f(m)) ++
      (for (m <- implClass.getMethods.toList; if (m.isAnnotationPresent(classOf[consume]))) yield f(m))
    }
  }
}
