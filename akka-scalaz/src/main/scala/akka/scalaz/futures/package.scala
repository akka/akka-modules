package akka.scalaz

import scalaz._
import Scalaz._

import akka.actor.Actor.TIMEOUT
import akka.dispatch.{Future, DefaultCompletableFuture}

import java.util.concurrent.TimeUnit

import futures.conversions._

package object futures extends Futures
    with CompletableFutures
    with ActorRefs
    with conversions.Promises
    with conversions.Function0s
    with conversions.Function1s {

  def nanosToMillis(in: Long): Long = TimeUnit.NANOSECONDS.toMillis(in)

  implicit def FutureFunctor(implicit exec: FutureExecuter) = new Functor[Future] {
    def fmap[A, B](r: Future[A], f: A => B): Future[B] = {
      val fb = new DefaultCompletableFuture[B](nanosToMillis(r.timeoutInNanos))
      r onComplete (fa => fa.result.cata(a => exec(try {fb.completeWithResult(f(a))} catch {case e => fb.completeWithException(e)}),
        fa.exception.foreach(fb.completeWithException)))
      fb
    }
  }

  implicit def FutureBind(implicit exec: FutureExecuter) = new Bind[Future] {
    def bind[A, B](r: Future[A], f: A => Future[B]) = {
      val fb = new DefaultCompletableFuture[B](nanosToMillis(r.timeoutInNanos))
      r onComplete (fa => fa.result.cata(a => exec(try {f(a).onComplete(fb.completeWith(_))} catch {case e => fb.completeWithException(e)}),
        fa.exception.foreach(fb.completeWithException)))
      fb
    }
  }

  implicit def FuturePure(implicit exec: FutureExecuter) = new Pure[Future] {
    def pure[A](a: => A) = future(a)
  }

  implicit def FutureApply(implicit exec: FutureExecuter) = FunctorBindApply[Future]

  implicit def FutureEach(implicit exec: FutureExecuter) = new Each[Future] {
    def each[A](e: Future[A], f: A => Unit) = e onComplete (_.result foreach (r => exec(f(r))))
  }

  implicit def FutureSemigroup[A](implicit exec: FutureExecuter, smA: Semigroup[A]): Semigroup[Future[A]] =
    semigroup ((fa, fb) => (fa <**> fb)(_ |+| _))

  implicit def FutureZero[A](implicit exec: FutureExecuter, zeroA: Zero[A]): Zero[Future[A]] = zero(∅[A].pure[Future])

  implicit def FutureCojoin: Cojoin[Future] = new Cojoin[Future] {
    def cojoin[A](a: Future[A]) = future(a)(executer.InlineExecuter)
  }

  implicit def FutureCopure: Copure[Future] = new Copure[Future] {
    def copure[A](a: Future[A]) = a.get
  }

  def future[A](a: => A, timeout: Long = TIMEOUT)(implicit exec: FutureExecuter): Future[A] = {
    val f = new DefaultCompletableFuture[A](timeout)
    exec(try {f.completeWithResult(a)} catch {case e => f.completeWithException(e)})
    f
  }

  def futureMap[M[_], A, B](ma: M[A])(f: A => B)(implicit t: Traverse[M], exec: FutureExecuter): Future[M[B]] =
    ma ∘ (f.future) sequence

  def futureBind[M[_], A, B](ma: M[A])(f: A => M[B])(implicit m: Monad[M], t: Traverse[M], exec: FutureExecuter): Future[M[B]] =
    futureMap(ma)(f).map(_.join)
}
