/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.mongo

import akka.stm._
import akka.util.Logging
import akka.persistence.common._
import akka.actor.{newUuid}

import com.mongodb.DBObject
import org.bson.types.ObjectId

// TODO: Config driven replacement for MongoStorage object which picks an implementation

object MongoStorage extends MongoStorage {
  val backend = MongoNativeBackend
} 

object MongoClassicStorage extends BytesStorage {
  val backend = MongoClassicBackend
}

object MongoNativeBackend extends Backend[Any] {
  val sortedSetStorage = None //Some(MongoNativeSetStorageBackend)
  val refStorage = None // Some(MongoNativeRefStorageBackend)
  val vectorStorage = None //Some(MongoNativeVectorStorageBackend)
  val queueStorage = None // Some(MongoNativeQueueStorageBackend)
  val mapStorage = None //Some(MongoNativeMapStorageBackend)
}

object MongoClassicBackend extends Backend[Array[Byte]] {
  val sortedSetStorage = None
  val refStorage = Some(MongoClassicStorageBackend)
  val vectorStorage = Some(MongoClassicStorageBackend)
  val queueStorage = None
  val mapStorage = Some(MongoClassicStorageBackend)
}

/** Native Storage layer for MongoDB,
 * using Mongo's persistence and atomic operators.
 *
 * Generates native MongoDB ObjectIDs as well for Transaction ID.
 * 
 * @author Brendan W. McAdams <brendan@10gen.com>
 * @version 1.0, 01/14/11
 * @since 1.1
 */
trait MongoStorage extends Storage with Logging {
  type ElementType = Any

  def newSortedSet() = getSortedSet(new ObjectId)

  def newSortedSet(id: String) = getSortedSet(id)

  def newSortedSet(oid: ObjectId) = getSortedSet(oid)

  def getSortedSet(id: String) = getSortedSet(new ObjectId(id))

  def getSortedSet(oid: ObjectId) =  
    backend.storageManager.getSortedSet(oid.toString, backend.sortedSetStorage match {
        case None => throw new UnsupportedOperationException
        case Some(store) => new MongoPersistentSortedSet {
          val _id = oid
          val storage = store
        }
    })



  def newQueue() = getQueue(new ObjectId)

  def newQueue(id: String) = getQueue(id)

  def newQueue(oid: ObjectId) = getQueue(oid)

  def getQueue(id: String) = getQueue(new ObjectId(id))
    
  def getQueue(oid: ObjectId) = 
    backend.storageManager.getQueue(oid.toString, backend.queueStorage match { 
        case None => throw new UnsupportedOperationException
        case Some(store) => new MongoPersistentQueue {
          val _id = oid
          val storage = store
        }
    })

  def newRef() = getRef(new ObjectId)

  def newRef(id: String) = getRef(id)

  def newRef(oid: ObjectId) = getRef(oid)

  def getRef(id: String) = getRef(new ObjectId(id))

  def getRef(oid: ObjectId) = 
    backend.storageManager.getRef(oid.toString, backend.refStorage match { 
        case None => throw new UnsupportedOperationException
        case Some(store) => new MongoPersistentRef {
          val _id = oid
          val storage = store
        }
    })

  def newVector() = getVector(new ObjectId)

  def newVector(id: String) = getVector(id)

  def newVector(oid: ObjectId) = getVector(oid)

  def getVector(id: String) = getVector(new ObjectId(id))

  def getVector(oid: ObjectId) = 
    backend.storageManager.getVector(oid.toString, backend.vectorStorage match { 
        case None => throw new UnsupportedOperationException
        case Some(store) => new MongoPersistentVector {
          val _id = oid
          val storage = store
        }
    })


  def newMap() = getMap(new ObjectId)

  def newMap(id: String) = getMap(id)

  def newMap(oid: ObjectId) = getMap(oid)

  def getMap(id: String): PersistentMap[Any, Any] = getMap(new ObjectId(id))

  def getMap(oid: ObjectId): PersistentMap[Any, Any] = 
    backend.storageManager.getMap(oid.toString, backend.mapStorage match { 
        case None => throw new UnsupportedOperationException
        case Some(store) => new MongoPersistentMap {
          val _id = oid
          val storage = store
        }
    })

    
}

