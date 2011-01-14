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

import scala.util.control.Exception._

trait MongoNativeStorageBackend extends MongoBackendConfiguration with Logging {

  val COLLECTION = "akka_stm"
  val STORAGE_KEY: String
  val VERSION_KEY: String = "version"

  protected def _id(txnID: String) =
    MongoDBObject(
      "_id" -> MongoDBObject("transaction_id" -> txnID, 
                             "akkaSTM" -> true)
    )

  protected def queryFor[T](txnID: String)(body: (MongoDBObject, Option[DBObject]) => T): T = {
    val q = _id(txnID)
    val dbo = coll.findOne(q, MongoDBObject(STORAGE_KEY -> true, VERSION_KEY -> true, "_id" -> false))
    // TODO - Version verification?
    body(q, dbo)
  }

  /** 
   * Utility method similar to Casbah's request() operator but 
   * uses the WriteConcern / WriteResult semantic instead. 
   * Wraps a write operation block (Block must return WriteResult) e.g.
   * last call is to save/insert/update/remove and throws any error.
   *
   * Casbah will have a version of this in a future release
   */
   protected def mongoRequest(op: MongoCollection => WriteResult) = 
    op(coll).getLastError.throwOnError

}

private[akka] object MongoNativeMapStorageBackend extends 
  MapStorageBackend[Any, Any] with
  MongoNativeStorageBackend with
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
      builder += ("$set" -> MongoDBObject("%s.%s".format(STORAGE_KEY, k) -> v)) 
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
    mongoRequest { _ update(_id(txnID), $unset ("%s.%s".format(STORAGE_KEY, key)) ) } 

  def removeMapStorageFor(txnID: String) = mongoRequest { _ remove(_id(txnID)) }


}


// vim: set ts=2 sw=2 sts=2 et:
