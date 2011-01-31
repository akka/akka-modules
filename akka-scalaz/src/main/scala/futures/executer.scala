package akka.scalaz.futures

import akka.config.Config._

package object executer {
  implicit object SpawnExecuter extends FutureExecuter {
    import akka.actor.Actor.spawn
    def apply(f: => Unit): Unit = spawn(f)
  }

  implicit object HawtExecuter extends FutureExecuter {
    import org.fusesource.hawtdispatch.ScalaDispatch._
    val queue = globalQueue
    def apply(f: => Unit): Unit = queue(f)
  }

  implicit object InlineExecuter extends FutureExecuter {
    def apply(f: => Unit): Unit = f
  }
}

trait FutureExecuter {
  def apply(f: => Unit): Unit
}

object FutureExecuter {
  import executer._

  implicit val DefaultExecuter: FutureExecuter = config.getString("akka.scalaz.executer", "inline") match {
    case "spawn" => SpawnExecuter
    case "hawt" => HawtExecuter
    case "inline" => InlineExecuter
    case _ => error("Invalid config for akka.scalaz.executer")
  }
}
