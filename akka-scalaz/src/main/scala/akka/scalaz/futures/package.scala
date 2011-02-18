package akka.scalaz

import scalaz._
import Scalaz._

import akka.actor.Actor.TIMEOUT
import akka.dispatch.{Future, DefaultCompletableFuture}

import java.util.concurrent.TimeUnit
import TimeUnit.{NANOSECONDS => NANOS, MILLISECONDS => MILLIS}

import futures.conversions._

package object futures extends Futures
    with CompletableFutures
    with ActorRefs
    with conversions.Promises
    with conversions.Function0s
    with conversions.Function1s {

  implicit def FutureFunctor = new Functor[Future] {
    def fmap[A, B](r: Future[A], f: A => B): Future[B] = {
      val fb = new DefaultCompletableFuture[B](r.timeoutInNanos, NANOS)
      r onComplete (_.value.foreach(_.fold(fb.completeWithException, a => fb.complete(try {Right(f(a))} catch {case e => Left(e)}))))
      fb
    }
  }

  implicit def FutureBind = new Bind[Future] {
    def bind[A, B](r: Future[A], f: A => Future[B]) = {
      val fb = new DefaultCompletableFuture[B](r.timeoutInNanos, NANOS)
      r onComplete (_.value.foreach(_.fold(fb.completeWithException, a => try {f(a).onComplete(fb.completeWith(_))} catch {case e => fb.completeWithException(e)})))
      fb
    }
  }

  implicit def FuturePure = new Pure[Future] {
    def pure[A](a: => A) = executer.Inline.future(a)
  }

  implicit def FutureEach = new Each[Future] {
    def each[A](e: Future[A], f: A => Unit) = e onComplete (_.result foreach (r => f(r)))
  }

  implicit def FutureSemigroup[A: Semigroup]: Semigroup[Future[A]] =
    semigroup ((fa, fb) => (fa <**> fb)(_ |+| _))

  implicit def FutureZero[A: Zero]: Zero[Future[A]] = zero(∅[A].pure[Future])

  implicit def FutureCojoin: Cojoin[Future] = new Cojoin[Future] {
    def cojoin[A](a: Future[A]) = executer.Inline.future(a)
  }

  implicit def FutureCopure: Copure[Future] = new Copure[Future] {
    def copure[A](a: Future[A]) = a.get
  }

  def future[A](a: => A, timeout: Long = TIMEOUT, timeunit: TimeUnit = MILLIS)(implicit exec: FutureExecuter): Future[A] =
    exec.future(a, timeout, timeunit)

  def futureMap[M[_], A, B](ma: M[A])(f: A => B)(implicit t: Traverse[M], exec: FutureExecuter): Future[M[B]] =
    ma map (a => exec.future(f(a))) sequence

  def futureBind[M[_], A, B](ma: M[A])(f: A => M[B])(implicit m: Monad[M], t: Traverse[M], exec: FutureExecuter): Future[M[B]] =
    futureMap(ma)(f).map(_.join)
}
