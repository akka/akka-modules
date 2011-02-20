package akka.scalaz.futures

import scalaz._
import Scalaz._

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary
import scalacheck.{ ScalazProperties, ScalazArbitrary, ScalaCheckBinding }
import ScalaCheckBinding._
import ScalazArbitrary._

import akka.dispatch._
import akka.actor.Actor
import Actor._

import akka.util.Logging

class AkkaFuturesSpec extends WordSpec with ShouldMatchers with Checkers with Logging {

  implicit def FutureEqual[A: Equal] = Scalaz.equal[Future[A]]((a1, a2) => a1.get ≟ a2.get)

  implicit def FutureArbitrary[A](implicit arb: Arbitrary[A], exec: FutureExecuter): Arbitrary[Future[A]] = arb map ((a: A) => exec.future(a))

  "A Future" when {
    "using InlineExecuter" should {
      import executer.Inline
      behave like aFuture
    }

    "using SpawnExecuter" should {
      import executer.Spawn
      behave like aConcurrentFuture
    }

    "using HawtExecuter" should {
      import executer.Hawt
      behave like aConcurrentFuture
    }
  }

  def aFunctor(implicit exec: FutureExecuter) {
    import ScalazProperties.Functor._
    "satisfy the functor law of identity" in check(identity[Future, Int])
    "satisfy the functor law of associativity" in check(associative[Future, Int, Int, Int])
  }

  def aMonad(implicit exec: FutureExecuter) {
    import ScalazProperties.Monad._
    "satisfy the monad law of left identity" in check(leftIdentity[Future, Int, Int])
    "satisfy the monad law of right identity" in check(rightIdentity[Future, Int])
    "satisfy the monad law of associativity" in check(associativity[Future, Int, Int, Int])
  }

  def anApplicative(implicit exec: FutureExecuter) {
    import ScalazProperties.Applicative._
    "satisfy the applicative law of identity" in check(identity[Future, Int])
    "satisfy the applicative law of composition" in check(composition[Future, Int, Int, Int])
    "satisfy the applicative law of homomorphism" in check(homomorphism[Future, Int, Int])
    "satisfy the applicative law of interchange" in check(interchange[Future, Int, Int])
  }

  def aSemigroup(implicit exec: FutureExecuter) {
    import ScalazProperties.Semigroup._
    "satisfy the semigroup law of associativity" in check(associative[Future[Int]])
  }

  def aMonoid(implicit exec: FutureExecuter) {
    import ScalazProperties.Monoid._
    "satisfy the monoid law of identity" in check(identity[Future[Int]])
  }

  def aFuture(implicit exec: FutureExecuter) {
    behave like aFunctor
    behave like aMonad
    behave like anApplicative
    behave like aSemigroup
    behave like aMonoid

    "have scalaz functor instance" in {
      val f1 = future(5 * 5)
      val f2 = f1 map (_ * 2)
      val f3 = f2 map (_ * 10)
      val f4 = f1 map (_ / 0)
      val f5 = f4 map (_ * 10)

      f2.get should equal(50)
      f3.get should equal(500)
      evaluating(f4.get) should produce[ArithmeticException]
      evaluating(f5.get) should produce[ArithmeticException]
    }

    "have scalaz bind instance" in {
      val f1 = future(5 * 5)
      val f2 = f1 >>= (n => future(n * 2))
      val f3 = f2 >>= (n => future(n * 10))
      val f4 = f1 >>= (n => future(n / 0))
      val f5 = f4 >>= (n => future(n * 10))

      f2.get should equal(50)
      f3.get should equal(500)
      evaluating(f4.get) should produce[ArithmeticException]
      evaluating(f5.get) should produce[ArithmeticException]
    }

    "have scalaz apply instance" in {
      val f1 = future(5 * 5)
      val f2 = f1 map (_ * 2)
      val f3 = f2 map (_ / 0)

      (f1 |@| f2)(_ * _).get should equal(1250)
      (f1 |@| f2).tupled.get should equal(25, 50)
      evaluating((f1 |@| f2 |@| f3)(_ * _ * _).get) should produce[ArithmeticException]
      evaluating((f3 |@| f2 |@| f1)(_ * _ * _).get) should produce[ArithmeticException]
      (f1 <|**|> (f2, f1)).get should equal(25, 50, 25)
    }

    "have scalaz comonad instance" in {
      val f = future("Result") =>> (_ map (_.toUpperCase)) >>= (_ map (s => s + s))
      f.get should equal("RESULTRESULT")
    }

    "calculate fib seq" in {
      def seqFib(n: Int): Int = if (n < 2) n else seqFib(n - 1) + seqFib(n - 2)

      def fib(n: Int): Future[Int] =
        if (n < 30)
          future(seqFib(n))
        else
          (fib(n - 1) |@| fib(n - 2))(_ + _)

      fib(40).get should equal(102334155)
    }

    "traverse a list into a future" in {
      val result = (1 to 1000).toList.traverse(n => future(n * 10)).get
      result should have size (1000)
      result.head should equal(10)
    }

    "map a list in parallel" in {
      val result = futureMap((1 to 1000).toList)(10*).get
      result should have size (1000)
      result.head should equal(10)
    }

    "reduce a list of futures" in {
      val list = (1 to 100).toList.fpure[Future]
      list.reduceLeft((a, b) => (a |@| b)(_ + _)).get should equal(5050)
    }

    "fold into a future" in {
      val list = (1 to 100).toList
      list.foldLeftM(0)((b, a) => future(b + a)).get should equal(5050)
    }

    "convert to Validation" in {
      val r1 = (future("34".toInt) |@| future("150".toInt) |@| future("12".toInt))(_ + _ + _)
      r1.liftValidation.get should equal(Success(196))
      val r2 = (future("34".toInt) |@| future("hello".toInt) |@| future("12".toInt))(_ + _ + _)
      r2.liftValidation.get.fail.map(_.toString).validation should equal(Failure("java.lang.NumberFormatException: For input string: \"hello\""))
    }

    "for-comprehension" in {
      val r1 = for {
        x1 <- future("34".toInt)
        x2 <- future("150".toInt)
        x3 <- future("12".toInt)
      } yield x1 + x2 + x3

      r1.get should equal(196)

      val r2 = for {
        x1 <- future("34".toInt)
        x2 <- future("hello".toInt)
        x3 <- future("12".toInt)
      } yield x1 + x2 + x3

      evaluating(r2.get) should produce[NumberFormatException]
    }

    "compose" in {
      val f = ((_: String).toInt).future
      val g = ((_: Int) * 2).future
      val h = ((_: Int) * 10).future

      (f apply "3" get) should equal(3)
      (f >=> g apply "3" get) should equal(6)
      (f >=> h apply "3" get) should equal(30)
      (f >=> g >=> h apply "3" get) should equal(60)
      (f >=> (g &&& h) apply "3" get) should equal(6, 30)
      ((f *** f) >=> (g *** h) apply ("3", "7") get) should equal(6, 70)
      evaluating(f >=> g >=> h apply "blah" get) should produce[NumberFormatException]
      evaluating((f *** f) >=> (g *** h) apply ("3", "blah") get) should produce[NumberFormatException]
    }

    "compose with actors" in {
      val a1 = actorOf[DoubleActor].start
      val a2 = actorOf[ToStringActor].start
      val l = (1 to 5).toList

      (l traverse a1).get should equal(List(2, 4, 6, 8, 10))
      (l traverse (a1 >=> a2)).get should equal(List("Int: 2", "Int: 4", "Int: 6", "Int: 8", "Int: 10"))
      (l traverse (a1 &&& (a1 >=> a2))).get should equal(List((2, "Int: 2"), (4, "Int: 4"), (6, "Int: 6"), (8, "Int: 8"), (10, "Int: 10")))

      val f = ((_: String).toInt).future
      val g = ((_: Int) * 2).future
      val h = ((_: Int) * 10).future

      ((f *** f) >=> (g *** h) >=> (a1 *** a2) apply ("3", "7") get) should equal(12, "Int: 70")

      val fn = (n: Int) => (a1 >=> a2) apply n map {
        case "Int: 10" => "10"
        case _         => "failure"
      } >>= (f >=> g)
      fn(5).get should equal(20)
      evaluating(fn(10).get) should produce[NumberFormatException]

      a1.stop
      a2.stop
    }

    "Plus" in {
      val r1 = future(1)
      val r2 = future(2)
      val e1 = future(1 / 0)
      val e2 = future("Hello".toInt)

      (r1 <+> r2).get should equal(1)
      (r2 <+> e1).get should equal(2)
      (e2 <+> r1).get should equal(1)
      (e1 <+> e2 <+> r1 <+> r2).get should equal(1)
    }

    "Semigroups" in {
      (future(3) |+| future(4)).get should equal(7)
      (future(List(1, 2, 3)) |+| future(List(4, 5, 6))).get should equal(List(1, 2, 3, 4, 5, 6))
    }

    "Monoids" in {
      val doubler = ((_: Int) * 2).future

      (List(1, 2, 3, 4, 5).fpure[Future].asMA.sum).get should equal(15)
      (List(1, 2, 3, 4, 5) foldMapDefault doubler).get should equal(30)
      (nil[Int] foldMapDefault doubler).get should equal(0)

      (1 +>: 2 +>: 3 +>: doubler(10) |+| doubler(100) map (_ |+| 300)).get should equal(526)

      1.unfold[Future, String](x => (x < 5).option((x.toString, x + 1))).get should equal("1234")
    }

    // Taken from Haskell example, performance is very poor, this is only here as a test
    "quicksort a list" in {
      val rnd = new scala.util.Random(1)
      val list = List.fill(1000)(rnd.nextInt)

      def qsort[T: Order](in: List[T]): Future[List[T]] = in match {
        case Nil           => nil.pure[Future]
        case x :: Nil      => List(x).pure[Future]
        case x :: y :: Nil => (if (x lt y) List(x, y) else List(y, x)).pure[Future]
        case x :: xs       => (future(qsort(xs.filter(x.gt))).join |@| x.pure[Future] |@| future(qsort(xs.filter(x.lte))).join)(_ ::: _ :: _)
      }

      qsort(list).get should equal(list.sorted)
    }
  }

  def aConcurrentFuture(implicit exec: FutureExecuter) {
    behave like aFuture

    "have a resetable timeout" in {
      future("test").timeout(100).get should equal("test")
      evaluating(future({ Thread.sleep(500); "test" }).timeout(100).get) should produce[FutureTimeoutException]
    }
  }
}

class DoubleActor extends Actor {
  def receive = {
    case i: Int => self reply (i * 2)
  }
}

class ToStringActor extends Actor {
  def receive = {
    case i: Int => self reply ("Int: " + i)
  }
}

