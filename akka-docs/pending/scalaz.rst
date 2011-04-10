Scalaz
======

**This is a work in progress and is mostly an outline at this point. More detailed information to come soon.**

Introduction
------------

The akka-scalaz module provides implementations of most Scalaz type classes. The intended audience of this documentation is someone who is not familiar with Scalaz, as the methods are the same as long as the relevant type classes are implemented. At the moment only Future, functions returning Future, and Actors that return a Future benefit from this module and should behave similarily to Scalaz's Promise.

Futures
-------

TODO: Add examples

To use this module, you must import scalaz and the type classes for Future:

.. code-block:: scala

  import scalaz._
  import Scalaz._
  import akka.scalaz.futures._

Note: Whenever an additional collection is required to explain the use of a method, List is used. Any other monad/functor/foldable/etc can be used in it’s placed, as long as the applicable type classes are defined in scalaz or are in scope elsewhere.

map
^^^

.. code-block:: scala

  Future[A] map (A => B): Future[B]
  Future[A] >| (=> B): Future[B]
  Future[List[A]] map2 (A => B): Future[List[B]]
  List[Future[A]] map2 (A => B): List[Future[B]]

flatMap
^^^^^^^

.. code-block:: scala

  Future[A] flatMap (A => Future[B]): Future[B]
  Future[A] >>= (A => Future[B]): Future[B]
  Future[Future[A]] join: Future[A]

foreach
^^^^^^^

.. code-block:: scala

  Future[A] foreach (A => Unit): Unit
  Future[A] |>| (A => Unit): Unit

applicative
^^^^^^^^^^^

.. code-block:: scala

  (Future[A] <**> Future[B])((A, B) => C): Future[C]
  Future[A] <|*|> Future[B]: Future[(A, B)]
  Future[A] |@| Future[B]: ApplicativeBuilder[Future, A, B]

traverse
^^^^^^^^

.. code-block:: scala

  List[A] traverse (A => Future[B]): Future[List[B]]
  List[Future[A]] sequence: Future[List[A]]

fold
^^^^

.. code-block:: scala

  List[A].foldl(Future[B])((Future[B], A) => Future[B]): Future[B]
  List[A] foldLeftM(B)((B, A) => Future[B]): Future[B]
  List[Future[A]] foldl1 ((Future[A], Future[A]) => Future[A]): Option[Future[A]]
  List[A].foldr(Future[B])((A, => Future[B]) => Future[B]): Future[B]
  List[A] foldRightM(B)((B, A) => Future[B]): Future[B]
  List[Future[A]] foldr1 ((Future[A], => Future[A]) => Future[A]): Option[Future[A]]

monoid
^^^^^^

.. code-block:: scala

  List[A] foldMapDefault (A => Future[B]): Future[B]
  List[Future[A]] collapse: Future[A]
  List[A] foldMap (A => Future[B]): Future[B]
  List[Future[A]] sum: Future[A]
  List[Future[A]] sumr: Future[A]
  Future[A] |+| Future[A]: Future[A]
  A +>: Future[A]: Future[A]

composition
^^^^^^^^^^^

.. code-block:: scala

  (A => Future[B]) >=> (B => Future[C]): A => Future[C]

misc
^^^^

.. code-block:: scala

  Future[A] <+> Future[A]: Future[A]
  Future[A] getOrElseM Future[Option[A]]: Future[A]
  Future[A] copure: A
  Future[A] fpure[List]: Future[List[A]]

Actors
------

An ActorRef can be implicitly converted into a function "Any => Future[Any]" and used wherever that function is accepted. For example:

.. code-block:: scala

  ActorRef >=> ActorRef: Any => Future[Any]
  Future[A] flatMap ActorRef: Future[Any]
  List[A] traverse ActorRef: Future[List[Any]]

Concurrency
-----------

TODO: Explain when and where the given functions are applied to the value of a Future, and how to manipulate this.
TODO: Configuration options

Type Classes
------------

Future
^^^^^^

Pure
Functor
Bind
Each
Monad (implicitly from Pure and Bind)
Apply (implicitly from Functor and Bind)
Applicative (implicitly from Pure and Apply)
Cojoin
Copure
Comonad (implicitly from Functor, Cojoin, and Copure)

The following type classes are available if the Future's contained type also implements the same type class:
Semigroup
Zero
Monoid (implicitly from Semigroup and Zero)
