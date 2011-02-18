package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future
import akka.actor.ActorRef

sealed trait ActorRefW extends PimpedType[ActorRef] with Function1[Any, Future[Any]] {
  def future: Kleisli[Future, Any, Any] = kleisli(value !!! _)

  def apply(in: Any): Future[Any] = value !!! in
}

trait ActorRefs {
  implicit def ActorRefTo(a: ActorRef): ActorRefW = new ActorRefW {
    val value = a
  }

  implicit def ActorRefToKleisli(a: ActorRef): Kleisli[Future, Any, Any] = kleisli(x => a !!! x)

  implicit def ActorRefMAB(a: ActorRef) = KleisliMAB(a)
}
