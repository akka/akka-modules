package akka.scalaz.futures
package conversions

import scalaz._
import Scalaz._

import akka.dispatch.Future

sealed trait Function1W[T, R] {
  val k: T => R

  def future(implicit p: Pure[Future]): Kleisli[Future, T, R] = k.kleisli(p)
}

trait Function1s {
  implicit def Function1To[T, R](f: T => R): Function1W[T, R] = new Function1W[T, R] {
    val k = f
  }

  implicit def Function1From[T, R](f: Function1W[T, R]): T => R = f.k
}
