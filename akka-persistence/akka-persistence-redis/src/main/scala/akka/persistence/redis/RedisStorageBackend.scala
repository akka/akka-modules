/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.redis

import akka.stm._
import akka.persistence.common._
import akka.util.Logging
import akka.config.Config.config

import com.redis._
import com.redis.cluster._
import com.redis.serialization._
import com.redis.serialization.Parse.Implicits.parseByteArray

/**
 * A module for supporting Redis based persistence.
 * <p/>
 * The module offers functionality for:
 * <li>Persistent Maps</li>
 * <li>Persistent Vectors</li>
 * <li>Persistent Refs</li>
 * <p/>
 * @author <a href="http://debasishg.blogspot.com">Debasish Ghosh</a>
 */
private [akka] object RedisStorageBackend extends
  MapStorageBackend[Array[Byte], Array[Byte]] with
  VectorStorageBackend[Array[Byte]] with
  RefStorageBackend[Array[Byte]] with
  QueueStorageBackend[Array[Byte]] with
  SortedSetStorageBackend[Array[Byte]] with
  Logging {

  // need an explicit definition in akka-conf
  val nodes = config.getList("akka.persistence.redis.cluster")


  def connect(): Either[RedisCluster, RedisClientPool] =
    nodes match {
      case Seq() =>
        // no cluster defined
        val REDIS_SERVER_HOSTNAME = config.getString("akka.persistence.redis.hostname", "127.0.0.1")
        val REDIS_SERVER_PORT = config.getInt("akka.persistence.redis.port", 6379)
        Right(new RedisClientPool(REDIS_SERVER_HOSTNAME, REDIS_SERVER_PORT))

      case s =>
        // with cluster
        import com.redis.cluster._
        log.info("Running on Redis cluster")
        Left(new RedisCluster(nodes: _*) {
          val keyTag = Some(NoOpKeyTag)
        })
    }

  var clients = connect()

  implicit val format = Format{
    case (name: String, key: Array[Byte]) => ((name+":").getBytes("UTF-8") ++ key).array
  }

  val parseRedisKey = Parse{ b =>
    val i = b.indexOf(':'.toByte)
    (new String(b.slice(0,i), "UTF-8"), b.slice(i+1,b.length))
  }

  /**
   * Map storage in Redis.
   * <p/>
   * Maps are stored as key/value pairs in redis.
   */
  def insertMapStorageEntryFor(name: String, key: Array[Byte], value: Array[Byte]): Unit = 
    insertMapStorageEntriesFor(name, List((key, value)))

  def insertMapStorageEntriesFor(name: String, entries: List[Tuple2[Array[Byte], Array[Byte]]]): Unit = 
    mset(entries.map(e => ((name, e._1), e._2)))

  private [this] def mset(entries: List[((String, Array[Byte]), Array[Byte])]): Unit = withErrorHandling {
    client => {
      entries.foreach {e =>
        client.set(e._1, e._2)
      }
    }
  }

  def removeMapStorageFor(name: String): Unit = withErrorHandling {
    client => {
      client.keys("%s:*".format(name)) match {
        case None =>
          throw new NoSuchElementException(name + " not present")
        case Some(keys) =>
          keys.foreach(k => client.del(k.get))
      }
    }
  }

  def removeMapStorageFor(name: String, key: Array[Byte]): Unit = withErrorHandling {
    client => {
      client.del((name, key))
    }
  }

  def getMapStorageEntryFor(name: String, key: Array[Byte]): Option[Array[Byte]] = withErrorHandling {
    client => {
      client.get((name, key))
        .orElse(throw new NoSuchElementException(new String(key, "UTF-8") + " not present"))
      }
    }

  def getMapStorageSizeFor(name: String): Int = withErrorHandling {
    client => {
      client.keys("%s:*".format(name)).map(_.length).getOrElse(0)
    }
  }

  def getMapStorageFor(name: String): List[(Array[Byte], Array[Byte])] = withErrorHandling {
    implicit val parser = parseRedisKey
    client => {
      client.keys[(String, Array[Byte])]("%s:*".format(name))
        .map { keys =>
          keys.map(key => (key.get._2, client.get[Array[Byte]](key.get).get)).toList
        }.getOrElse {
          throw new NoSuchElementException(name + " not present")
        }
      }
  }

  implicit object ByteArrayOrdering extends Ordering[Array[Byte]] {
    def compare(x: Array[Byte], y: Array[Byte]) = {
      x.view.zipAll(y, 0.toByte, 0.toByte).map(b => b._1.toInt - b._2.toInt).find(_ != 0).getOrElse(0)
    }
  }

  def getMapStorageRangeFor(name: String, start: Option[Array[Byte]],
                            finish: Option[Array[Byte]],
                            count: Int): List[(Array[Byte], Array[Byte])] = {

    import scala.collection.immutable.TreeMap
    val wholeSorted =
      TreeMap(getMapStorageFor(name): _*)

    if (wholeSorted isEmpty) List()

    ((start, finish, count): @unchecked) match {
      case ((Some(s), Some(e), _)) =>
        wholeSorted.range(s, e).toList
      case ((Some(s), None, c)) if c > 0 =>
        wholeSorted.from(s).iterator.take(count).toList
      case ((Some(s), None, _)) =>
        wholeSorted.from(s).toList
      case ((None, Some(e), _)) =>
        wholeSorted.until(e).toList
    }
  }

  def insertVectorStorageEntryFor(name: String, element: Array[Byte]): Unit = withErrorHandling {
    client => {
      client.lpush(name, element)
    }
  }

  def insertVectorStorageEntriesFor(name: String, elements: List[Array[Byte]]): Unit = {
    elements.foreach(insertVectorStorageEntryFor(name, _))
  }

  def updateVectorStorageEntryFor(name: String, index: Int, elem: Array[Byte]): Unit = withErrorHandling {
    client => {
      client.lset(name, index, elem)
    }
  }

  def getVectorStorageEntryFor(name: String, index: Int): Array[Byte] = withErrorHandling {
    client => {
      client.lindex(name, index)
        .getOrElse {
          throw new NoSuchElementException(name + " does not have element at " + index)
        }
      }
  }

  /**
   * if <tt>start</tt> and <tt>finish</tt> both are defined, ignore <tt>count</tt> and
   * report the range [start, finish)
   * if <tt>start</tt> is not defined, assume <tt>start</tt> = 0
   * if <tt>start</tt> == 0 and <tt>finish</tt> == 0, return an empty collection
   */
  def getVectorStorageRangeFor(name: String, start: Option[Int], finish: Option[Int], count: Int): List[Array[Byte]] = withErrorHandling {
    client => {
      val s = start.getOrElse(0)
      val cnt = finish.map(f => if (f >= s) (f - s) else count).getOrElse(count)
      if (s == 0 && cnt == 0) List()
      else
      client.lrange(name, s, s + cnt - 1).getOrElse(throw new NoSuchElementException(name + " does not have elements in the range specified")).flatten
    }
  }

  def getVectorStorageSizeFor(name: String): Int = withErrorHandling {
    client => {
      client.llen(name).getOrElse { throw new NoSuchElementException(name + " not present") }
    }
  }

  def insertRefStorageFor(name: String, element: Array[Byte]): Unit = withErrorHandling {
    client => {
      client.set(name, element)
    }
  }

  def insertRefStorageFor(name: String, element: String): Unit = withErrorHandling {
    client => {
      client.set(name, element)
    }
  }

  def getRefStorageFor(name: String): Option[Array[Byte]] = withErrorHandling {
    client => {
      client.get(name)
    }
  }

  // add to the end of the queue
  def enqueue(name: String, item: Array[Byte]): Option[Int] = withErrorHandling {
    client => {
      client.rpush(name, item)
    }
  }

  // pop from the front of the queue
  def dequeue(name: String): Option[Array[Byte]] = withErrorHandling {
    client => {
      client.lpop(name)
        .orElse {
          throw new NoSuchElementException(name + " not present")
        }
      }
  }

  // get the size of the queue
  def size(name: String): Int = withErrorHandling {
    client => {
      client.llen(name).getOrElse { throw new NoSuchElementException(name + " not present") }
    }
  }

  // return an array of items currently stored in the queue
  // start is the item to begin, count is how many items to return
  def peek(name: String, start: Int, count: Int): List[Array[Byte]] = withErrorHandling {
    client => {
      count match {
        case 1 =>
          client.lindex(name, start).orElse(throw new NoSuchElementException("No element at " + start)).toList
        case n =>
          client.lrange(name, start, start + count - 1).getOrElse(
            throw new NoSuchElementException("No element found between " + start + " and " + (start + count - 1))).flatten
      }
    }
  }

  // completely delete the queue
  def remove(name: String): Boolean = withErrorHandling {
    client => {
      client.del(name).map(_ == 1).getOrElse(false)
    }
  }

  // add item to sorted set identified by name
  def zadd(name: String, zscore: String, item: Array[Byte]): Boolean = withErrorHandling {
    client => {
      client.zadd(name, zscore.toDouble, item)
    }
  }

  // remove item from sorted set identified by name
  def zrem(name: String, item: Array[Byte]): Boolean = withErrorHandling {
    client => {
      client.zrem(name, item)
    }
  }

  // cardinality of the set identified by name
  def zcard(name: String): Int = withErrorHandling {
    client => {
      client.zcard(name).getOrElse { throw new NoSuchElementException(name + " not present") }
    }
  }

  def zscore(name: String, item: Array[Byte]): Option[Float] = withErrorHandling {
    client => {
      client.zscore(name, item).map(_.toFloat)
    }
  }

  def zrange(name: String, start: Int, end: Int): List[Array[Byte]] = withErrorHandling {
    client => {
      client.zrange(name, start, end, RedisClient.ASC).getOrElse(throw new NoSuchElementException(name + " not present"))
    }
  }

  def zrangeWithScore(name: String, start: Int, end: Int): List[(Array[Byte], Float)] = withErrorHandling {
    client => {
      client.zrangeWithScore(name, start, end, RedisClient.ASC)
        .map(_.map { case (elem, score) => (elem, score.toFloat) })
        .getOrElse {
          throw new NoSuchElementException(name + " not present")
        }
      }
  }

  def flushDB = withErrorHandling{
    client => {
      client.flushdb
    }
  }

  def close = 
    if (clients isLeft) clients.left.get.close
    else clients.right.get.close

  private def withErrorHandling[T](body: RedisClient => T): T = {
    try {
      if (clients isLeft) body(clients.left.get)
      else {
        clients.right.get.withClient { 
          client => body(client)
        }
      }
    } catch {
      case e: RedisConnectionException => {
        if (clients isLeft) {
          clients = connect()
          body(clients.left.get)
        } else {
          clients.right.get.pool.close
          clients = connect()
          clients.right.get.withClient { 
            client => body(client)
          }
        }
      }
      case e: java.lang.NullPointerException =>
        throw new StorageException("Could not connect to Redis server")
      case e =>
        throw new StorageException("Error in Redis: " + e.getMessage)
    }
  }
}
