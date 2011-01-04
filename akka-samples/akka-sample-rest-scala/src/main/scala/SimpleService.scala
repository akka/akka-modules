/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package sample.rest.scala

import akka.actor.{SupervisorFactory, Actor}
import akka.actor.Actor._
import akka.stm._
import akka.stm.TransactionalMap
import akka.persistence.cassandra.CassandraStorage
import akka.config.Supervision._
import akka.util.Logging
import scala.xml.NodeSeq
import java.lang.Integer
import java.nio.ByteBuffer
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.{GET, POST, Path, Produces, WebApplicationException, Consumes,PathParam}

class Boot {
  val factory = SupervisorFactory(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      Supervise(actorOf[SimpleServiceActor],Permanent) ::
      Supervise(actorOf[PersistentSimpleServiceActor],Permanent)
      :: Nil))
  factory.newInstance.start
}

/**
 * Try service out by invoking (multiple times):
 * <pre>
 * curl http://localhost:9998/scalacount
 * </pre>
 * Or browse to the URL from a web browser.
 */
@Path("/scalacount")
class SimpleService {
  @GET
  @Produces(Array("text/html"))
  def count = {
    //Fetch the first actor of type SimpleServiceActor
    //Send it the "Tick" message and expect a NodeSeq back
    val result = for{a <- registry.actorFor[SimpleServiceActor]
                     r <- (a !! "Tick").as[NodeSeq]} yield r
    //Return either the resulting NodeSeq or a default one
    result getOrElse <error>Error in counter</error>
  }
}

class SimpleServiceActor extends Actor {
  private val KEY = "COUNTER"
  private var hasStartedTicking = false
  private val storage = TransactionalMap[String, Integer]()

  def receive = {
    case "Tick" => if (hasStartedTicking) {
      val count = atomic {
        val current = storage.get(KEY).get.asInstanceOf[Integer].intValue
        val updated = current + 1
        storage.put(KEY, new Integer(updated))
        updated
      }
      self.reply(<success>Tick:{count}</success>)
    } else {
      atomic {
        storage.put(KEY, new Integer(0))
      }
      hasStartedTicking = true
      self.reply(<success>Tick: 0</success>)
    }
  }
}

/**
 * Try service out by invoking (multiple times):
 * <pre>
 * curl http://localhost:9998/persistentscalacount
 * </pre>
 * Or browse to the URL from a web browser.
 */
@Path("/persistentscalacount")
class PersistentSimpleService {
  @GET
  @Produces(Array("text/html"))
  def count = {
    //Fetch the first actor of type PersistentSimpleServiceActor
    //Send it the "Tick" message and expect a NodeSeq back
    val result = for{a <- registry.actorFor[PersistentSimpleServiceActor]
                     r <- (a !! "Tick").as[NodeSeq]} yield r
    //Return either the resulting NodeSeq or a default one
    result getOrElse <error>Error in counter</error>
  }
}

class PersistentSimpleServiceActor extends Actor {
  private val KEY = "COUNTER"
  private var hasStartedTicking = false
  private lazy val storage = CassandraStorage.newMap

  def receive = {
    case "Tick" => if (hasStartedTicking) {
      val count = atomic {
        val bytes = storage.get(KEY.getBytes).get
        val current = Integer.parseInt(new String(bytes, "UTF8"))
        val updated = current + 1
        storage.put(KEY.getBytes, (updated).toString.getBytes)
        updated
      }
//      val bytes = storage.get(KEY.getBytes).get
//      val counter = ByteBuffer.wrap(bytes).getInt
//      storage.put(KEY.getBytes, ByteBuffer.allocate(4).putInt(counter + 1).array)
      self.reply(<success>Tick:{count}</success>)
    } else {
      atomic {
        storage.put(KEY.getBytes, "0".getBytes)
      }
//      storage.put(KEY.getBytes, Array(0.toByte))
      hasStartedTicking = true
      self.reply(<success>Tick: 0</success>)
    }
  }
}