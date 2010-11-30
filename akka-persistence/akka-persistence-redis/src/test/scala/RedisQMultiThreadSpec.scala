package akka.persistence.redis

import org.junit.{Test, Before}
import org.junit.Assert._

import akka.actor.{Actor, ActorRef}
import Actor._
import akka.stm._

import akka.persistence.common.PersistentQueue

/**
 * A persistent actor based on Redis queue storage.
 * <p/>
 * Needs a running Redis server.
 * @author <a href="http://debasishg.blogspot.com">Debasish Ghosh</a>
 */

case class ENQ(msg: String)
case object DEQ

class NQActor(storage: PersistentQueue[Array[Byte]]) extends Actor {
  def receive = { case message => atomic { atomicReceive(message) } }

  def atomicReceive: Receive = {
    // enqueue
    case ENQ(msg) => 
      storage.enqueue(msg.getBytes)
      self.reply(true)

    // size
    case SZ => self.reply(storage.size)
  }
}

class DQActor(storage: PersistentQueue[Array[Byte]]) extends Actor {
  def receive = { case message => atomic { atomicReceive(message) } }

  def atomicReceive: Receive = {
    // enqueue
    case DEQ => 
      val d = new String(storage.dequeue)
      self.reply(d)

    // size
    case SZ => self.reply(storage.size)
  }
}

import org.scalatest.junit.JUnitSuite
class RedisQMultiThreadSpec extends JUnitSuite {
  @Test
  def testSuccessfulNQ = {
    RedisStorageBackend.flushDB
    val st = RedisStorage.newQueue
    val eqa = actorOf(new NQActor(st))
    val dqa = actorOf(new DQActor(st))
    eqa.start
    dqa.start
    eqa !! ENQ("a-123")
    eqa !! ENQ("a-124")
    eqa !! ENQ("a-125")
    val t = (eqa !! SZ).as[Int].get
    assertTrue(3 == t)
    assertEquals("a-123", (dqa !! DEQ).get)
    assertEquals("a-124", (dqa !! DEQ).get)
    assertEquals("a-125", (dqa !! DEQ).get)
    try {
      dqa !! DEQ
    } catch { 
      case e: NoSuchElementException => {}
    }
    eqa !! ENQ("a-126")
    val accs = (1 until 10).map("acc_%d".format(_))
    accs.map(eqa !! ENQ(_))
    val s = (eqa !! SZ).as[Int].get
    assertTrue(10 == s)
    for (i <- 1 until 10) dqa !! DEQ
    assertEquals(1, (eqa !! SZ).as[Int].get)
  }
}
