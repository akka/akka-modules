package akka.persistence.terrastore

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

import akka.persistence.common._
import terrastore.client.connection.resteasy.HTTPConnectionFactory
import terrastore.client.{BucketOperation, TerrastoreClient}
import terrastore.client.connection.NoSuchKeyException
import collection.Map
import collection.immutable.Iterable
import collection.immutable.HashMap
import org.apache.commons.codec.binary.Base64

/**
 * @author <a href="http://www.davidgreco.it">David Greco</a>
 */
private[akka] object TerrastoreStorageBackend extends CommonStorageBackend {

  class TerrastoreAccess(val store: String) extends KVStorageBackendAccess {

    var client: TerrastoreClient = _
    var bucket: BucketOperation = _

    initAccess

    def initAccess() = {
      client = new TerrastoreClient("http://localhost:8080", new HTTPConnectionFactory());
      bucket = client.bucket(store)
    }

    def put(key: Array[Byte], value: Array[Byte]) = {
      val ks = Base64.encodeBase64String(key)
      val vs = Base64.encodeBase64String(value)
      println(ks+" "+vs)
      bucket.key(ks).put(new Value(vs))
    }

    def get(key: Array[Byte]): Array[Byte] = {
      try {
        val ks = Base64.encodeBase64String(key)
        val vs = bucket.key(ks).get(classOf[Value]).getValue
        println(ks+" "+vs)
        Base64.decodeBase64(vs)
      }
      catch {
        case e: NoSuchKeyException => null
        case e => throw e
      }
    }

    def get(key: Array[Byte], default: Array[Byte]): Array[Byte] = {
      try {
        val ks = Base64.encodeBase64String(key)
        val vs = bucket.key(ks).get(classOf[Value]).getValue
        println(ks+" "+vs)
        Base64.decodeBase64(vs)
      }
      catch {
        case e: NoSuchKeyException => default
        case e => throw e
      }
    }

    def getAll(keys: Iterable[Array[Byte]]): Map[Array[Byte], Array[Byte]] = {
      var result = new HashMap[Array[Byte], Array[Byte]]
      keys.foreach{
        key =>
          val value = get(key)
          Option(value) match {
            case Some(value) => result += key -> value
            case None => ()
          }
      }
      result
    }

    def delete(key: Array[Byte]) = {
      val ks = Base64.encodeBase64String(key)
      println(ks)
      bucket.key(ks).remove
    }

    def drop() = {
      val bucket = client.bucket(store)
      bucket.clear
    }
  }

  def queueAccess = new TerrastoreAccess("queues")

  def mapAccess = new TerrastoreAccess("maps")

  def vectorAccess = new TerrastoreAccess("vectors")

  def refAccess = new TerrastoreAccess("refs")

}

class Value(var value: String) {

  def this() = this (null)

  def getValue = value

  def setValue(value: String): Unit = {
    this.value = value
  }

}
