package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future
import akka.actor.ActorRef

sealed trait ActorRefW extends PimpedType[ActorRef] {
  def future: Kleisli[Future, Any, Any] = kleisli(value !!! _)
}

trait ActorRefs extends ActorRefsLow {
  implicit def ActorRefTo(a: ActorRef): ActorRefW = new ActorRefW {
    val value = a
  }

  implicit def ActorRefToFunction1(a: ActorRef): Any => Future[Any] = (x: Any) => a !!! x
}

trait ActorRefsLow {
  implicit def ActorRefToKleisli(a: ActorRef): Kleisli[Future, Any, Any] = kleisli(x => a !!! x)
}
