/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.mongo

import akka.stm._
import akka.persistence.common._
import akka.actor.{newUuid}

import com.mongodb.casbah.commons.Imports._
import com.mongodb.DBObject
import org.bson.types.ObjectId

/** Mongo friendlier implementation of Transactional
 * which creates a Mongo ObjectId for the 'real' 
 * transaction ID, but conforms to Akka's APIs req.
 * that they provide a UUID string by invoking `toString`
 *
 * @author Brendan W. McAdams <brendan@10gen.com>
 * @version 1.0, 01/14/11
 * @since 1.1
 * @todo Make sure I'm not causing any weird bugs by _id <-> uuid
 */
trait MongoTransactional extends Transactional {
  val _id: ObjectId
  override val uuid = _id.toString
}

object MongoAnyOrdering {
  implicit object AnyOrdering extends Ordering[Any] {
    def compare(o1: Any, o2: Any) = o1.toString.compare(o2.toString)
  }
}

trait MongoPersistentMap extends PersistentMap[Any, Any] with MongoTransactional {
  import scala.collection.immutable.{SortedMap, HashMap, TreeMap, StringLike}
  
  type T = StringLike[String]

  
  private def replayAllKeys: SortedMap[Any, Any] = {
    import MongoAnyOrdering._

    val fromStorage = TreeMap(storage.getMapStorageFor(uuid): _*)

    val (keysAdded, keysRemoved) = keysInCurrentTx.map {
      case (_, k) => (k, getCurrentValue(k))
    }.partition(_._2.isDefined)

    val inStorageRemovedInTx =
      keysRemoved.filterKeys(k => existsInStorage(k).isDefined)

    (fromStorage -- inStorageRemovedInTx) ++ keysAdded.map { case (k, v) => (k, v.get) }
  }

  def slice(start: Option[Any], finish: Option[Any], count: Int): List[(Any, Any)] = try {
    val newMap = replayAllKeys

    if (newMap isEmpty) List[(Any, Any)]()

    ((start, finish, count): @unchecked) match {
      case ((Some(s), Some(e), _)) => newMap.range(s, e).toList
      case ((Some(s), None, c)) if c > 0 => newMap.from(s).take(count).toList
      case ((Some(s), None, _)) => newMap.from(s).toList
      case ((None, Some(e), _)) => newMap.until(e).toList
    }


  } catch { case e: Exception => Nil }
  
  def iterator: Iterator[(Any, Any)] = {
    new Iterator[(Any, Any)] {
      private var elements = replayAllKeys

      def next: (Any, Any) = synchronized {
        val (k, v) = elements.head
        elements = elements.tail
        (k, v)
      }

      def hasNext: Boolean = synchronized { !elements.isEmpty }
    }
  }


  import scala.collection.JavaConversions.asJavaIterator

  def javaIterator = asJavaIterator(iterator)
  
  def toEquals(k: Any) = k.toString

}

trait MongoPersistentVector extends PersistentVector[Any] with MongoTransactional 

trait MongoPersistentRef extends PersistentRef[Any] with MongoTransactional 

trait MongoPersistentSortedSet extends PersistentSortedSet[Any] with MongoTransactional

trait MongoPersistentQueue extends PersistentQueue[Any] with MongoTransactional


// vim: set ts=2 sw=2 sts=2 et:
