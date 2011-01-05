/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.mongo

import akka.stm._
import akka.persistence.common._
import akka.util.Logging
import akka.config.Config.config

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoURI 
import com.mongodb.{ServerAddress, WriteResult}

import scala.util.control.Exception._

/**
 * A module for supporting MongoDB based persistence.
 * <p/>
 * The module offers functionality for:
 * <li>Persistent Maps</li>
 * <li>Persistent Vectors</li>
 * <li>Persistent Refs</li>
 * <p/>
 * @author <a href="http://debasishg.blogspot.com">Debasish Ghosh</a>
 */
private[akka] object MongoStorageBackend extends
  MapStorageBackend[Array[Byte], Array[Byte]] with
  VectorStorageBackend[Array[Byte]] with
  RefStorageBackend[Array[Byte]] with
  Logging {

  val KEY = "__key"
  val REF = "__ref"
  val COLLECTION = "akka_coll"
  
  /**
   * Parse the URI if defined and set the Hostname, dbname and port settings to it
   * Does not support setting write concern in options yet until Java driver does.
   */
  config.getString("akka.persistence.mongodb.uri") match {
    case None | null | Some("") => {
      log.debug("No URI configured, deferring config.")        
    }
    case Some(uri) => handling(classOf[IllegalArgumentException]) by { e =>
      throw new Exception("Unable to parse MongoURI '%s': '%s'".format(uri, e.getMessage), e)
    } apply {
      val address = MongoURI(uri)
      if (address.hosts.size == 1) {
        val host = address.hosts(1)
        if (address.username != null && !address.username.isEmpty) 
          config.setString("akka.persistence.mongodb.username", address.username)
        if (address.password != null && !address.password.isEmpty) 
          config.setString("akka.persistence.mongodb.username", address.password.mkString)

        if (host.indexOf(':') >= 0) {
          val x = host.split(':')
          config.setString("akka.persistence.mongodb.hostname", x(0))
          config.setString("akka.persistence.mongodb.port", x(1))
        } else config.setString("akka.persistence.mongodb.hostname", host)
      } else {
        // If they give us a list they'll need to enable replica mode or ports get lost
        config.setString("akka.persistence.mongodb.hostname", address.hosts.mkString(","))
      }
    }
  }

  
  /** Mongo Config Values */
  val HOSTNAME = config.getString("akka.persistence.mongodb.hostname", "127.0.0.1")
  val DBNAME = config.getString("akka.persistence.mongodb.dbname", "testdb")
  val PORT = config.getInt("akka.persistence.mongodb.port", 27017)

  /** Optional Authentication support */
  val USERNAME = config.getString("akka.persistence.mongodb.username")
  val PASSWORD = config.getString("akka.persistence.mongodb.password")

  /** 
   * Support for Replica Pair and Sets modes 
   * If REPLICA_PAIR is enabled we try to cut on a , into a left and right addresses
   * If REPLICA_SETS is enabled we parse on , into a List<String> addresses 
   */
  val REPLICA_PAIR = config.getBool("akka.persistence.mongodb.replica_mode.pairs", false)
  val REPLICA_SETS = config.getBool("akka.persistence.mongodb.replica_mode.sets", false)

  /** 
   * Write concern related config
   *    For the context of STM, it is best to use a w=1 (same as calling getLastError
   *    which happens in the request{} blocks.)
   *    However, many users want to config this and may even want w=2
   *   @see http://api.mongodb.org/java/2.5-pre-/com/mongodb/WriteConcern.html
   */
  /** 
   * Default 1 -- Wait for 1 server to commit the write before returning.  
   * If Replica Pairs or Sets is enabled this default will become 2 instead of 1
   */
  val WRITE_CONCERN_W = config.getInt("akka.persistence.mongodb.write_concern.w", 
                                      if (REPLICA_PAIR || REPLICA_SETS) 2 else 1)
  /** Default 0 -- Wait / block forever for writes to return. */
  val WRITE_CONCERN_WTIMEOUT = 
    config.getInt("akka.persistence.mongodb.write_concern.wtimeout", 0)

  /** Defauilt false, if true flushes mongo data to disk on each write before
   * returning.  Setting this to true should only be done by those who know 
   * what they are doing
   */
  val WRITE_CONCERN_FSYNC = 
    config.getBool("akka.persistence.mongodb.write_concern_fsync", false)

  val db: MongoDB = getMongoDB 

  // Authenticate if necessary (password can be null technically)
  USERNAME foreach { login =>
    require(db.authenticate(login, PASSWORD.getOrElse("")), 
            "Mongo Authentication failed with provided credentials.")
  }

  val coll: MongoCollection = db(COLLECTION)

  protected def getMongoDB: MongoDB = {
    log.debug("Setting up MongoConnection")
    assume(!(REPLICA_PAIR && REPLICA_SETS), 
          "Replica Pairs and Sets cannot both be enabled: Pick one!")

    def _setAddr(str: String) = if (str.indexOf(':') >= 0) {
      val h = str.split(':')
      new ServerAddress(h(0), h(1).toInt)
    } else new ServerAddress(str)

    val conn = if (REPLICA_PAIR) {
      log.info("Mongo Replica Pair mode enabled.")
      val hosts = HOSTNAME.split(',')
      require(hosts.size == 2, 
              "Replica Pairs may have no less than or more than 2 members (L, R)")

      val l = _setAddr(hosts(0))
      log.debug("Left Replica Pair member: %s", l)
      val r = _setAddr(hosts(1))
      log.debug("Right Replica Pair member: %s", r)
      MongoConnection(l, r)
    } 
    else if (REPLICA_SETS) {
      log.info("Mongo Replica Sets mode enabled.")
      // No requirement on size, replica sets can connect to just one box.
      // However, if there was no comma sep we need the port
      if (HOSTNAME.indexOf(',') >= 0) {
        val hosts = HOSTNAME.split(',')
        val hostList = hosts.toList map { _setAddr }
        log.debug("Host list for Replica Set: %s", hostList)
        MongoConnection(hostList)
      } else MongoConnection(List(new ServerAddress(HOSTNAME, PORT)))
    } 
    else {
      log.info("Standard Mongo Connection mode enabled.")
      MongoConnection(HOSTNAME, PORT)
    }
    // Setup Write Concern
    val wc = WriteConcern(WRITE_CONCERN_W, WRITE_CONCERN_WTIMEOUT, WRITE_CONCERN_FSYNC)
    conn.writeConcern = wc
    conn(DBNAME)
  }

  def drop() { db.dropDatabase() }

  def insertMapStorageEntryFor(name: String, key: Array[Byte], value: Array[Byte]) {
    insertMapStorageEntriesFor(name, List((key, value)))
  }

  def insertMapStorageEntriesFor(name: String, entries: List[(Array[Byte], Array[Byte])]) {
    val q: DBObject = MongoDBObject(KEY -> name)
    coll.findOne(q) match {
      case Some(dbo) =>
        entries.foreach { case (k, v) => dbo += new String(k) -> v }
        mongoRequest { _ update(q, dbo, true, false) }
      case None =>
        val builder = MongoDBObject.newBuilder
        builder += KEY -> name
        entries.foreach { case (k, v) => builder += new String(k) -> v }
        mongoRequest { _ += builder.result.asDBObject }
    }
  }

  def removeMapStorageFor(name: String): Unit = {
    val q: DBObject = MongoDBObject(KEY -> name)
    mongoRequest { _ remove(q) }
  }


  private def queryFor[T](name: String)(body: (MongoDBObject, Option[DBObject]) => T): T = {
    val q = MongoDBObject(KEY -> name)
    body(q, coll.findOne(q))
  }

  def removeMapStorageFor(name: String, key: Array[Byte]): Unit = queryFor(name) { (q, dbo) =>
    dbo.foreach { d =>
      d -= new String(key)
      mongoRequest { _.update(q, d, true, false) }
    }
  }

  def getMapStorageEntryFor(name: String, key: Array[Byte]): Option[Array[Byte]] = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      d.getAs[Array[Byte]](new String(key))
    }.getOrElse(None)
  }

  def getMapStorageSizeFor(name: String): Int = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      d.size - 2 // need to exclude object id and our KEY
    }.getOrElse(0)
  }

  def getMapStorageFor(name: String): List[(Array[Byte], Array[Byte])]  = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      for {
        (k, v) <- d.toList
        if k != "_id" && k != KEY
      } yield (k.getBytes, v.asInstanceOf[Array[Byte]])
    }.getOrElse(List.empty[(Array[Byte], Array[Byte])])
  }

  def getMapStorageRangeFor(name: String, start: Option[Array[Byte]],
                            finish: Option[Array[Byte]],
                            count: Int): List[(Array[Byte], Array[Byte])] = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      // get all keys except the special ones
      val keys = d.keys
                  .filter(k => k != "_id" && k != KEY)
                  .toList
                  .sortWith(_ < _)

      // if the supplied start is not defined, get the head of keys
      val s = start.map(new String(_)).getOrElse(keys.head)

      // if the supplied finish is not defined, get the last element of keys
      val f = finish.map(new String(_)).getOrElse(keys.last)

      // slice from keys: both ends inclusive
      val ks = keys.slice(keys.indexOf(s), scala.math.min(count, keys.indexOf(f) + 1))
      ks.map(k => (k.getBytes, d.getAs[Array[Byte]](k).get))
    }.getOrElse(List.empty[(Array[Byte], Array[Byte])])
  }

  def insertVectorStorageEntryFor(name: String, element: Array[Byte]) = {
    insertVectorStorageEntriesFor(name, List(element))
  }

  def insertVectorStorageEntriesFor(name: String, elements: List[Array[Byte]]) = {
    // lookup with name
    val q: DBObject = MongoDBObject(KEY -> name)

    coll.findOne(q) match {
      // exists : need to update
      case Some(dbo) =>
        dbo -= KEY
        dbo -= "_id"
        val listBuilder = MongoDBList.newBuilder

        // expensive!
        listBuilder ++= (elements ++ dbo.toSeq.sortWith((e1, e2) => (e1._1.toInt < e2._1.toInt)).map(_._2))

        val builder = MongoDBObject.newBuilder
        builder += KEY -> name
        builder ++= listBuilder.result
        mongoRequest { _ update(q, builder.result.asDBObject, true, false) }

      // new : just add
      case None =>
        val listBuilder = MongoDBList.newBuilder
        listBuilder ++= elements

        val builder = MongoDBObject.newBuilder
        builder += KEY -> name
        builder ++= listBuilder.result
        mongoRequest { _ += builder.result.asDBObject }
    }
  }

  def updateVectorStorageEntryFor(name: String, index: Int, elem: Array[Byte]) = queryFor(name) { (q, dbo) =>
    dbo.foreach { d =>
      d += ((index.toString, elem))
      mongoRequest { _.update(q, d, true, false) }
    }
  }

  def getVectorStorageEntryFor(name: String, index: Int): Array[Byte] = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      d(index.toString).asInstanceOf[Array[Byte]]
    }.getOrElse(Array.empty[Byte])
  }

  /**
   * if <tt>start</tt> and <tt>finish</tt> both are defined, ignore <tt>count</tt> and
   * report the range [start, finish)
   * if <tt>start</tt> is not defined, assume <tt>start</tt> = 0
   * if <tt>start</tt> == 0 and <tt>finish</tt> == 0, return an empty collection
   */
  def getVectorStorageRangeFor(name: String, start: Option[Int], finish: Option[Int], count: Int): List[Array[Byte]] = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      val ls = d.filter { case (k, v) => k != KEY && k != "_id" }
                .toSeq
                .sortWith((e1, e2) => (e1._1.toInt < e2._1.toInt))
                .map(_._2)

      val st = start.getOrElse(0)
      val cnt =
        if (finish.isDefined) {
          val f = finish.get
          if (f >= st) (f - st) else count
        }
        else count
      if (st == 0 && cnt == 0) List()
      ls.slice(st, st + cnt).asInstanceOf[List[Array[Byte]]]
    }.getOrElse(List.empty[Array[Byte]])
  }

  def getVectorStorageSizeFor(name: String): Int = queryFor(name) { (q, dbo) =>
    dbo.map { d => d.size - 2 }.getOrElse(0)
  }

  def insertRefStorageFor(name: String, element: Array[Byte]) = {
    // lookup with name
    val q: DBObject = MongoDBObject(KEY -> name)

    coll.findOne(q) match {
      // exists : need to update
      case Some(dbo) =>
        dbo += ((REF, element))
        mongoRequest { _ update(q, dbo, true, false) }

      // not found : make one
      case None =>
        val builder = MongoDBObject.newBuilder
        builder += KEY -> name
        builder += REF -> element
        mongoRequest { _ += builder.result.asDBObject }
    }
  }

  def getRefStorageFor(name: String): Option[Array[Byte]] = queryFor(name) { (q, dbo) =>
    dbo.map { d =>
      d.getAs[Array[Byte]](REF)
    }.getOrElse(None)
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
