package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future

sealed trait Function0W[A] {
  val k: () => A

  def future(implicit exec: FutureExecuter): Future[A] = exec.future(k.apply)
}

trait Function0s {
  implicit def Function0To[A](f: () => A): Function0W[A] = new Function0W[A] {
    val k = f
  }
}
