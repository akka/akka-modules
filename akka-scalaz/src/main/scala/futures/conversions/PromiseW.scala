package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._
import scalaz.concurrent.Promise

import akka.actor.Actor.TIMEOUT
import akka.dispatch.{Future, DefaultCompletableFuture}

sealed trait PromiseW[A] extends PimpedType[Promise[A]] {
  def toFuture: Future[A] = {
    val fa = new DefaultCompletableFuture[A](TIMEOUT)
    value to fa.completeWithResult
    fa
  }
}

trait Promises {
  implicit def PromiseTo[A](pa: Promise[A]): PromiseW[A] = new PromiseW[A] {
    val value = pa
  }
}
