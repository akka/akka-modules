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


trait MongoPersistentMap extends PersistentMap[Any, Any] with MongoTransactional {
  // TODO: Examine PersistentMapBinary and see if we need any of it's values
  
}

trait MongoPersistentVector extends PersistentVector[Any] with MongoTransactional 

trait MongoPersistentRef extends PersistentRef[Any] with MongoTransactional 

trait MongoPersistentSortedSet extends PersistentSortedSet[Any] with MongoTransactional

trait MongoPersistentQueue extends PersistentQueue[Any] with MongoTransactional


// vim: set ts=2 sw=2 sts=2 et:
