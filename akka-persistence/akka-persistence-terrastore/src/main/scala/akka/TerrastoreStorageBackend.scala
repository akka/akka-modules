package akka.persistence.terrastore

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

import akka.persistence.common._
import scala.collection.Map
import scala.collection.immutable.Iterable
import scala.collection.mutable.HashMap

/**
 * @author <a href="http://www.davidgreco.it">David Greco</a>
 */
private[akka] object TerrastoreStorageBackend extends CommonStorageBackend {

  class TerrastoreAccess(val store: String) extends KVStorageBackendAccess {

    def put(key: Array[Byte], value: Array[Byte]) = {
    }

    def get(key: Array[Byte]): Array[Byte] = {
      new Array[Byte](0)
    }

    def get(key: Array[Byte], default: Array[Byte]): Array[Byte] = {
      new Array[Byte](0)
    }

    def getAll(keys: Iterable[Array[Byte]]): Map[Array[Byte], Array[Byte]] = {
      new HashMap[Array[Byte], Array[Byte]]
    }

    def delete(key: Array[Byte]) = {
    }

    def drop() = {
    }

  }

  def queueAccess = new TerrastoreAccess("QUEUE")

  def mapAccess = new TerrastoreAccess("MAP")

  def vectorAccess = new TerrastoreAccess("VECTOR")

  def refAccess = new TerrastoreAccess("REF")

}
