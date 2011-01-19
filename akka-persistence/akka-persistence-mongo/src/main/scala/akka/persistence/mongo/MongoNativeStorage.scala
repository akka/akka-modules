/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.mongo

import akka.stm._
import akka.persistence.common._
import akka.util.Logging
import akka.config.Config.config

import com.mongodb.casbah.Imports._

import com.mongodb.{ServerAddress, WriteResult}

import scala.collection.immutable.SortedSet
import scala.util.control.Exception._

// TODO - Filter keys for things like $ and .
trait MongoNativeStorageBase extends MongoBackendConfiguration with Logging {

  val COLLECTION = "akka_stm"
  protected[this] val STORAGE_KEY: String

  protected def _id(txnID: String) =
    MongoDBObject(
      "_id" -> MongoDBObject("transaction_id" -> new ObjectId(txnID), 
                             "akkaSTM" -> true)
    )

  protected def _field(item: Any) = "%s.%s".format(STORAGE_KEY, item.toString)

  protected def queryFor[T](txnID: String)(body: (MongoDBObject, Option[DBObject]) => T): T = {
    val q = _id(txnID)
    val dbo = coll.findOne(q, MongoDBObject(STORAGE_KEY -> true, "_id" -> false))
    body(q, dbo)
  }

  /** 
   * Utility method similar to Casbah's request() operator but 
   * uses the WriteConcern / WriteResult semantic instead. 
   * Wraps a write operation block (Block must return WriteResult) e.g.
   * last call is to save/insert/update/remove and throws any error.
   *
   * Casbah will have a version of this in a future release
   * 
   * TODO - logging of any errors
   */
   protected def mongoRequest(op: MongoCollection => WriteResult) = 
    op(coll).getLastError.throwOnError

  def dropDB = coll.getDB.dropDatabase()
}

/*private[akka] object MongoNativeStorageBackend extends 
  MongoNativeMapStorageBackend with
  MongoNativeSortedSetStorageBackend */
  
private[akka] object MongoNativeMapStorageBackend extends MongoNativeMapStorageBackend

private[akka] trait MongoNativeMapStorageBackend extends 
  MapStorageBackend[Any, Any] with
  MongoNativeStorageBase with
  Logging {

  val STORAGE_KEY = "mapStorage"

  def getMapStorageEntryFor(txnID: String, key: Any) = queryFor(txnID) { (q, dbo) => 
    dbo.map { _.getAs[DBObject](STORAGE_KEY).get(key.toString) } orElse(None)
  }
  
  def getMapStorageFor(txnID: String) = queryFor(txnID) { (q, dbo) => 
    dbo.map { d =>
      val dbObj: DBObject = d.getAs[DBObject](STORAGE_KEY).getOrElse(MongoDBObject.empty)
      dbObj.toList: List[(String, Any)]
    } getOrElse(List.empty[(String, Any)])
  }
  
  def getMapStorageRangeFor(txnID: String, start: Option[Any], finish: Option[Any], 
                            count: Int) = queryFor(txnID) { (q, dbo) =>
    dbo.map { d =>
      val keys = d.getAs[DBObject](STORAGE_KEY)
                  .getOrElse(MongoDBObject.empty)
                  .keys
                  .toList
                  .sortWith(_ < _)

      // If the supplied start is undefined, get the head of the keys instead
      val s = start.map(_.toString).getOrElse(keys.head)

      // If the supplied finish is undefined, get the last of the keys instead
      val f = finish.map(_.toString).getOrElse(keys.last)

      keys.slice(keys.indexOf(s), 
                 scala.math.min(count, keys.indexOf(f) + 1)) map { k => (k, d.get(k)) }
    } getOrElse(List.empty[(String, Any)])
  }

  def getMapStorageSizeFor(txnID: String): Int = queryFor(txnID) { (q, dbo) => 
    dbo.map { _.getAs[DBObject](STORAGE_KEY).get.size } getOrElse(0)
  }


  def insertMapStorageEntriesFor(txnID: String, entries: List[(Any, Any)]) {
    val q = _id(txnID)
    val builder = MongoDBObject.newBuilder
    entries.foreach { case (k, v) => 
      builder += ("$set" -> (_field(k) -> v)) 
    }
    log.debug("Insert ID Calced: %s", q)
    // TODO Version and findAndModify increment support
    // This does NOT currently check if anyone has modified it out from underneath us
    // Uses $set to do in place modifies which is atomic and should be fast
    mongoRequest { _ update(q, builder.result, true, false) }
  }

  def insertMapStorageEntryFor(txnID: String, key: Any, value: Any) =
    insertMapStorageEntriesFor(txnID, List(key.toString -> value))


  def removeMapStorageFor(txnID: String, key: Any) =
    mongoRequest { _ update(_id(txnID), $unset (_field(key))) } 

  def removeMapStorageFor(txnID: String) = mongoRequest { _ remove(_id(txnID)) }

}

private[akka] object MongoNativeSortedSetStorageBackend extends MongoNativeSortedSetStorageBackend

// TODO - Less naive implementation of this ,as it doesn't deserialize the stringified
// and won't work with complex objects to use as map keys
private[akka] trait MongoNativeSortedSetStorageBackend extends 
  SortedSetStorageBackend[Any] with
  MongoNativeStorageBase with
  Logging {

  val STORAGE_KEY = "sortedSetStorage"

  implicit object ZScoredOrdering extends Ordering[(Any, Float)] {
    def compare(o1: (Any, Float), o2: (Any, Float)) = o1._2.compare(o2._2)
  }

  // add item to sorted set identified by name
  def zadd(txnID: String, zscore: String, item: Any): Boolean = {
    val q = _id(txnID)
    val doc = $set ("%s.%s".format(STORAGE_KEY, item.toString) -> zscore.toDouble)
    // Upsert: if it exists update else create it.
    mongoBoolUpdate { _ update(q, doc, true, false) }
  }

  // Remove item from sorted set identified by name
  def zrem(txnID: String, item: Any): Boolean = {
    val field = _field(item)
    // Use $exists to help us determine if it existed before remove or not
    val q = _id(txnID) ++ field $exists true
    val doc = $unset (field)
    mongoBoolUpdate { _ update(q, doc) }
  }


  // Cardinality of the set identified by name
  def zcard(txnID: String): Int = queryFor(txnID) { (q, dbo) =>
    // Just fetch the whole Set and count the keys (slight hackery)
    dbo.map { _.getAs[DBObject](STORAGE_KEY).size } getOrElse(0) 
  }

  // zscore of the item from sorted set, identified by name
  def zscore(txnID: String, item: Any): Option[Float] = queryFor(txnID) { (q, dbo) => 
    dbo.map { _.getAs[Double](_field(item)) map(_.toFloat) } getOrElse(None)
  }

  // zrange from the sorted set identified by name -- just the element
  def zrange(txnID: String, start: Int, end: Int): List[Any] =
    zrangeWithScore(txnID, start, end).map { _._1 }
  
  // zrange with score from the sorted set identified by name
  def zrangeWithScore(txnID: String, start: Int, end: Int): List[(Any, Float)] = queryFor(txnID) { 
    (q, dbo) => dbo.map { obj =>
      val builder = SortedSet.newBuilder[(Any, Float)]
      obj.getAs[DBObject](STORAGE_KEY).foreach { 
        _.map { case (k, v) => builder += (k -> v.asInstanceOf[Double].toFloat) } 
      } 
      val set = builder.result
      log.debug("Mapped Builder Set: %s", set)
      set.slice(start, end).toList
    } getOrElse(List.empty[(Any, Float)])
  }

  protected def mongoBoolUpdate(op: MongoCollection => WriteResult): Boolean = {
    val last = op(coll).getLastError 
    // If it was an exception the updatedExisting doesn't matter
    last.throwOnError 
    !last.getAs[Boolean]("updatedExisting").getOrElse(false)
  }

}
// vim: set ts=2 sw=2 sts=2 et:
