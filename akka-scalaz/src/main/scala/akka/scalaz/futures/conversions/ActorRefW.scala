package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future
import akka.actor.ActorRef

sealed trait ActorRefW extends PimpedType[ActorRef] {
  // include implicit parameter in order to overload method
  def future[A, B: Manifest]: Kleisli[Future, A, B] = kleisli((a: A) => value.!!![B](a))

  def future: Kleisli[Future, Any, Any] = kleisli(value !!! _)
}

trait ActorRefs {
  implicit def ActorRefTo(a: ActorRef): ActorRefW = new ActorRefW {
    val value = a
  }
}
