package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._
import scalaz.concurrent.{Promise, Strategy}

import akka.dispatch.{Future, DefaultCompletableFuture, FutureTimeoutException}

sealed trait FutureW[A] extends PimpedType[Future[A]] {
  def toValidation: Validation[Throwable, A] = {
    value.await
    value.result fold (success(_), failure(value.exception getOrElse (new FutureTimeoutException("Futures timed out after [" + nanosToMillis(value.timeoutInNanos) + "] milliseconds"))))
  }

  def toEither: Either[Throwable, A] = {
    value.await
    value.result fold (Right(_), Left(value.exception getOrElse (new FutureTimeoutException("Futures timed out after [" + nanosToMillis(value.timeoutInNanos) + "] milliseconds"))))
  }

  def toPromise(implicit s: Strategy): Promise[A] =
    promise(this get)

  def timeout(t: Long): Future[A] = {
    val f = new DefaultCompletableFuture[A](t)
    value onComplete (f.completeWith(_))
    f
  }

  def get: A = {
    value.await
    value.result getOrElse (throw value.exception getOrElse new FutureTimeoutException("Futures timed out after [" + nanosToMillis(value.timeoutInNanos) + "] milliseconds"))
  }

  def getOrElse[B >: A](default: => B): B =
    value.await.result getOrElse default
}

trait Futures {
  implicit def FutureTo[A](f: Future[A]): FutureW[A] = new FutureW[A] {
    val value = f
  }
}
