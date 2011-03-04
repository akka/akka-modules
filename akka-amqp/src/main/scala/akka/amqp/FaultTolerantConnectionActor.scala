/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp

import java.util.{TimerTask, Timer}
import java.io.IOException
import com.rabbitmq.client._
import akka.amqp.AMQP.ConnectionParameters
import akka.config.Supervision.{ Permanent, OneForOneStrategy }
import akka.actor.{Exit, Actor, EventHandler}

private[amqp] class FaultTolerantConnectionActor(connectionParameters: ConnectionParameters) extends Actor {
  import connectionParameters._

  self.id = "amqp-connection-%s".format(host)
  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]))

  val reconnectionTimer = new Timer("%s-timer".format(self.id))

  val connectionFactory: ConnectionFactory = new ConnectionFactory()
  connectionFactory.setHost(host)
  connectionFactory.setPort(port)
  connectionFactory.setUsername(username)
  connectionFactory.setPassword(password)
  connectionFactory.setVirtualHost(virtualHost)

  var connection: Option[Connection] = None

  protected def receive = {
    case Connect => connect
    case ChannelRequest => {
      connection match {
        case Some(conn) => {
          val chanel: Channel = conn.createChannel
          self.reply(Some(chanel))
        }
        case None => {
          EventHandler notifyListeners EventHandler.Warning(null, this, "Unable to create new channel - no connection")
          self.reply(None)
        }
      }
    }
    case ConnectionShutdown(cause) => {
      if (cause.isHardError) {
        // connection error
        if (cause.isInitiatedByApplication) {
          EventHandler notifyListeners EventHandler.Info(this, "ConnectionShutdown by application [%s]" format self.id)
        } else {
          EventHandler notifyListeners EventHandler.Error(cause, this, "ConnectionShutdown is hard error - self terminating")
          self ! new Exit(self, cause)
        }
      }
    }
  }

  private def connect = if (connection.isEmpty || !connection.get.isOpen) {
    try {
      connection = Some(connectionFactory.newConnection)
      connection.foreach {
        conn =>
          conn.addShutdownListener(new ShutdownListener {
            def shutdownCompleted(cause: ShutdownSignalException) = {
              self ! ConnectionShutdown(cause)
            }
          })
          import scala.collection.JavaConversions._
          self.linkedActors.values.iterator.foreach(_ ! conn.createChannel)
          notifyCallback(Connected)
      }
    } catch {
      case e: Exception =>
        connection = None
        reconnectionTimer.schedule(new TimerTask() {
          override def run = {
            notifyCallback(Reconnecting)
            self ! Connect
          }
        }, connectionParameters.initReconnectDelay)
    }
  }

  private def disconnect = {
    try {
      connection.foreach(_.close)
      notifyCallback(Disconnected)
    } catch {
      case e: IOException =>
        EventHandler notifyListeners EventHandler.Error(null, this, "Could not close AMQP connection %s:%s [%s]".format(host, port, self.id))
      case _ => ()
    }
    connection = None
  }

  private def notifyCallback(message: AMQPMessage) = {
    connectionCallback.foreach(cb => if (cb.isRunning) cb ! message)
  }

  override def postStop = {
    reconnectionTimer.cancel
    // make sure postStop is called on all linked actors so they can do channel cleanup before connection is killed
    val i = self.linkedActors.values.iterator
    while(i.hasNext) {
      val ref = i.next
      ref.stop
      self.unlink(ref)
    }
    disconnect
  }

  override def preRestart(reason: Throwable) = disconnect

  override def postRestart(reason: Throwable) = {
    notifyCallback(Reconnecting)
    connect
  }
}
