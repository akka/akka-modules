package akka.persistence.terrastore

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

import akka.persistence.common._
import terrastore.client.connection.resteasy.HTTPConnectionFactory
import terrastore.client.{BucketOperation, TerrastoreClient}
import java.nio.charset.Charset
import com.google.protobuf.ByteString
import terrastore.client.connection.NoSuchKeyException
import collection.Map
import collection.immutable.Iterable
import collection.immutable.HashMap

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
      val ks = ByteString.copyFrom(key)
      val vs = new String(value, Charset.forName("UTF8"))
      bucket.key(ks.toString).put(new Value(vs))
    }

    def get(key: Array[Byte]): Array[Byte] = {
      try {
        val ks = ByteString.copyFrom(key)
        val value = bucket.key(ks.toString).get(classOf[Value])
        value.getValue.getBytes(Charset.forName("UTF8"))
      }
      catch {
        case e: NoSuchKeyException => null
        case e => throw e
      }
    }

    def get(key: Array[Byte], default: Array[Byte]): Array[Byte] = {
      try {
        val ks = ByteString.copyFrom(key)
        val value = bucket.key(ks.toString).get(classOf[Value])
        value.getValue.getBytes(Charset.forName("UTF8"))
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
      val ks = ByteString.copyFrom(key)
      bucket.key(ks.toString).remove
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
