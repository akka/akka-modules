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

  def toPromise(implicit s: Strategy): Promise[Validation[Throwable, A]] =
    promise(this toValidation)

  def timeout(t: Long): Future[A] = {
    val f = new DefaultCompletableFuture[A](t)
    value onComplete (f.completeWith(_))
    f
  }

  def get: Validation[Throwable, A] =
    this toValidation

  def getOrThrow: A = {
    value.await
    value.result getOrElse (throw value.exception getOrElse new FutureTimeoutException("Futures timed out after [" + nanosToMillis(value.timeoutInNanos) + "] milliseconds"))
  }

  def fold[X](failure: Throwable => X = identity[Throwable] _, success: A => X = identity[A] _): X =
    this.toValidation fold (failure, success)

  def onCompleteFold(failure: Throwable => Unit = _ => (), success: A => Unit = _ => ()): Unit =
    value.onComplete(f => f.result.fold(success, f.exception.foreach(failure)))
}

trait Futures {
  implicit def FutureTo[A](f: Future[A]): FutureW[A] = new FutureW[A] {
    val value = f
  }
}
