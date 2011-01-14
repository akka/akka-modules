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

/** Common configuration trait for Mongo Backends
 * capable of reading the Akka config and 
 * generating the appropriate MongoDB Connection.
 * 
 * @author Brendan W. McAdams <brendan@10gen.com>
 * @version 1.0, 01/14/11
 * @since 1.1
 * 
 * @tparam akka 
 */
protected[akka] trait MongoBackendConfiguration extends Logging {

  val COLLECTION: String

  lazy val coll: MongoCollection = db(COLLECTION)

  lazy val db: MongoDB = getMongoDB 

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
   * Users can specify an Enum value *OR* the specific values.
   *   @see http://api.mongodb.org/java/2.5-pre-/com/mongodb/WriteConcern.html
   */

  val WRITE_CONCERN = config.getString("akka.persistence.mongodb.write_concern") match {
    case None | null | Some("") => {
      /** 
       * Default 1 -- Wait for 1 server to commit the write before returning.  
       * If Replica Pairs or Sets is enabled this default will become 2 instead of 1
       */
      val w = config.getInt("akka.persistence.mongodb.write_concern.w", 
                                          if (REPLICA_PAIR || REPLICA_SETS) 2 else 1)
      /** Default 0 -- Wait / block forever for writes to return. */
      val wtimeout = 
        config.getInt("akka.persistence.mongodb.write_concern.wtimeout", 0)

      /** Default false, if true flushes mongo data to disk on each write before
       * returning.  Setting this to true should only be done by those who know 
       * what they are doing
       */
      val fsync = 
        config.getBool("akka.persistence.mongodb.write_concern_fsync", false)

      WriteConcern(w, wtimeout, fsync)
    }
    case Some(wc_enum) => {
      log.debug("Attempting to parse Write Concern enum value '%s'", wc_enum)
      val wc = WriteConcern.valueOf(wc_enum)
      require(wc != null, "Unable to resolve Write Concern enum value '%s'")
      wc
    }
  }


  // Authenticate if necessary (password can be null technically)
  USERNAME foreach { login =>
    require(db.authenticate(login, PASSWORD.getOrElse("")), 
            "Mongo Authentication failed with provided credentials.")
  }


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
    conn.writeConcern = WRITE_CONCERN
    conn(DBNAME)
  }


}

// vim: set ts=2 sw=2 sts=2 et:
