package akka.scalaz.futures

import akka.config.Config._

import akka.actor.Actor.TIMEOUT
import akka.dispatch.{ Future, DefaultCompletableFuture }

import java.util.concurrent.TimeUnit
import TimeUnit.{ NANOSECONDS => NANOS, MILLISECONDS => MILLIS }

package object executer {
  implicit object Spawn extends FutureExecuter {
    import akka.actor.Actor.spawn
    def apply(f: => Unit): Unit = spawn(f)
  }

  implicit object Hawt extends FutureExecuter {
    import org.fusesource.hawtdispatch._
    val queue = globalQueue
    def apply(f: => Unit): Unit = queue(f)
  }

  implicit object Inline extends FutureExecuter {
    def apply(f: => Unit): Unit = f
  }
}

trait FutureExecuter {
  def apply(f: => Unit): Unit

  final def future[A](a: => A, timeout: Long = TIMEOUT, timeunit: TimeUnit = MILLIS): Future[A] = {
    val f = new DefaultCompletableFuture[A](timeout, timeunit)
    apply(f.complete(try { Right(a) } catch { case e => Left(e) }))
    f
  }

}

object FutureExecuter {
  import executer._

  implicit val DefaultExecuter: FutureExecuter = config.getString("akka.scalaz.executer", "spawn") match {
    case "spawn"  => Spawn
    case "hawt"   => Hawt
    case "inline" => Inline
    case _        => error("Invalid config for akka.scalaz.executer")
  }
}
