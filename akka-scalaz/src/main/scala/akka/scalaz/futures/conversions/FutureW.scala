package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._
import scalaz.concurrent.{ Promise, Strategy }

import akka.dispatch.{ Future, DefaultCompletableFuture, FutureTimeoutException }

import java.util.concurrent.TimeUnit
import TimeUnit.{ NANOSECONDS => NANOS, MILLISECONDS => MILLIS }

sealed trait FutureW[A] extends PimpedType[Future[A]] {

  def lift: Future[Either[Throwable, A]] = {
    val f = new DefaultCompletableFuture[Either[Throwable, A]](value.timeoutInNanos, NANOS)
    value onComplete (r => f.completeWithResult(r.value.get))
    f
  }

  def liftValidation: Future[Validation[Throwable, A]] = {
    val f = new DefaultCompletableFuture[Validation[Throwable, A]](value.timeoutInNanos, NANOS)
    value onComplete (r => f.completeWithResult(validation(r.value.get)))
    f
  }

  def liftValidationNel: Future[Validation[NonEmptyList[Throwable], A]] = {
    val f = new DefaultCompletableFuture[Validation[NonEmptyList[Throwable], A]](value.timeoutInNanos, NANOS)
    value onComplete (r => f.completeWithResult(validation(r.value.get).liftFailNel))
    f
  }

  def toPromise(implicit s: Strategy): Promise[A] =
    promise(this get)

  def timeout(t: Long): Future[A] = {
    val f = new DefaultCompletableFuture[A](t)
    value onComplete (f.completeWith(_))
    f
  }

  def getOrElse[B >: A](default: => B): B = try {
    value.await.value.flatMap(_.right.toOption) getOrElse default
  } catch {
    case f: FutureTimeoutException => default
  }

  def orElse[B >: A](b: => Future[B]): Future[B] = {
    val f = new DefaultCompletableFuture[B](value.timeoutInNanos, NANOS)
    value onComplete (_.value.foreach(v1 =>
      v1.fold(e1 => b onComplete (_.value.foreach(v2 =>
        v2.fold(e2 => f complete v1, r2 => f complete v2))), r1 => f complete v1)))
    f
  }

}

trait Futures {
  implicit def FutureTo[A](f: Future[A]): FutureW[A] = new FutureW[A] {
    val value = f
  }
}
