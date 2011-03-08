package akka.scalaz

import scalaz._
import Scalaz._

import akka.actor.Actor.TIMEOUT
import akka.dispatch.{ Future, DefaultCompletableFuture => DCF }

import java.util.concurrent.TimeUnit
import TimeUnit.{ NANOSECONDS => NANOS, MILLISECONDS => MILLIS }

import futures.conversions._

package object futures extends Futures
    with CompletableFutures
    with ActorRefs
    with conversions.Promises
    with conversions.Function0s
    with conversions.Function1s {

  implicit def FutureFunctor = new Functor[Future] {
    def fmap[A, B](r: Future[A], f: A => B): Future[B] = r map f
  }

  implicit def FutureBind = new Bind[Future] {
    def bind[A, B](r: Future[A], f: A => Future[B]) = r flatMap f
  }

  implicit def FuturePure = new Pure[Future] {
    def pure[A](a: => A): Future[A] = new DCF[A](TIMEOUT) complete (try { Right(a) } catch { case e => Left(e) })
  }

  implicit def FutureEach = new Each[Future] {
    def each[A](e: Future[A], f: A => Unit) = e foreach f
  }

  implicit def FuturePlus = new Plus[Future] {
    def plus[A](a1: Future[A], a2: => Future[A]): Future[A] = a1 orElse a2
  }

  implicit def FutureSemigroup[A: Semigroup]: Semigroup[Future[A]] =
    semigroup((fa, fb) => (fa <**> fb)(_ |+| _))

  implicit def FutureZero[A: Zero]: Zero[Future[A]] = zero(∅[A].pure[Future])

  implicit def FutureCojoin: Cojoin[Future] = new Cojoin[Future] {
    def cojoin[A](a: Future[A]) = a.pure[Future]
  }

  implicit def FutureCopure: Copure[Future] = new Copure[Future] {
    def copure[A](a: Future[A]) = a.get
  }
}
