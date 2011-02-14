package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.CompletableFuture

sealed trait CompletableFutureW[A] extends PimpedType[CompletableFuture[A]] {
  def complete(validation: Validation[Throwable, A]): Unit =
    value.complete(validation.either)
}

trait CompletableFutures {
  implicit def CompletableFutureTo[A](f: CompletableFuture[A]): CompletableFutureW[A] = new CompletableFutureW[A] {
    val value = f
  }
}
