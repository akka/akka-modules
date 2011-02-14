package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._
import scalaz.concurrent.{Promise, Strategy}

import akka.dispatch.{Future, DefaultCompletableFuture, FutureTimeoutException}

sealed trait FutureW[A] extends PimpedType[Future[A]] {
  def liftValidation: Future[Validation[Throwable, A]] = {
    val f = new DefaultCompletableFuture[Validation[Throwable, A]](nanosToMillis(value.timeoutInNanos))
    value onComplete (r => f.completeWithResult(Scalaz.validation(r.value.get)))
    f
  }

  def toPromise(implicit s: Strategy): Promise[A] =
    promise(this get)

  def timeout(t: Long): Future[A] = {
    val f = new DefaultCompletableFuture[A](t)
    value onComplete (f.completeWith(_))
    f
  }

  // Gives Future the same get method as Java Future and Scalaz Promise
  def get: A = value.await.resultOrException.get

  def getOrElse[B >: A](default: => B): B =
    value.awaitResult.flatMap(_.right.toOption) getOrElse default
}

trait Futures {
  implicit def FutureTo[A](f: Future[A]): FutureW[A] = new FutureW[A] {
    val value = f
  }
}
