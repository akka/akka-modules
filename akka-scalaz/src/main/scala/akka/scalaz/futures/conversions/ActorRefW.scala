package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future
import akka.actor.ActorRef

sealed trait ActorRefW extends PimpedType[ActorRef] {
  def future: Kleisli[Future, Any, Any] = kleisli(value !!! _)
}

trait ActorRefs {
  implicit def ActorRefTo(a: ActorRef): ActorRefW = new ActorRefW {
    val value = a
  }
}
