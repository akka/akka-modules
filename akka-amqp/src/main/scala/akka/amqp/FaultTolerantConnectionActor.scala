/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp

import java.util.{TimerTask, Timer}
import java.io.IOException
import com.rabbitmq.client._
import akka.amqp.AMQP.ConnectionParameters
import akka.config.Supervision.{ Permanent, OneForOneStrategy }
import akka.actor.{Scheduler, Exit, Actor}
import akka.event.EventHandler
import java.util.concurrent.{ScheduledFuture, TimeUnit}

private[amqp] class FaultTolerantConnectionActor(connectionParameters: ConnectionParameters) extends Actor {
  import connectionParameters._

  self.id = "amqp-connection"
  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]))

  val connectionFactory: ConnectionFactory = new ConnectionFactory()
  connectionFactory.setUsername(username)
  connectionFactory.setPassword(password)
  connectionFactory.setVirtualHost(virtualHost)

  var connection: Option[Connection] = None
  var reconnectionFuture: Option[ScheduledFuture[scala.AnyRef]] = None

  protected def receive = {
    case Connect => connect
    case ChannelRequest => {
      connection match {
        case Some(conn) => {
          val chanel: Channel = conn.createChannel
          self.reply(Some(chanel))
        }
        case None => {
          EventHandler notifyListeners EventHandler.Warning(this, "Unable to create new channel - no connection")
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
      EventHandler notifyListeners EventHandler.Info(this, "Connecting to one of [%s]" format addresses)
      connection = Some(connectionFactory.newConnection(addresses))
      connection.foreach {
        conn =>
          EventHandler notifyListeners EventHandler.Info(this, "Connected to [%s:%s]" format (conn.getHost, conn.getPort))
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
        EventHandler notifyListeners EventHandler.Error(e, this, "Unable to connect to one of [%s]" format addresses)
        EventHandler notifyListeners EventHandler.Info(this, "Reconnecting in %d ms " format initReconnectDelay)
        connection = None
        reconnectionFuture = Some(Scheduler.scheduleOnce(() => {
          notifyCallback(Reconnecting)
          self ! Connect
        }, initReconnectDelay, TimeUnit.MILLISECONDS))
    }
  }

  private def disconnect = {
    try {
      connection.foreach(_.close)
      notifyCallback(Disconnected)
    } catch {
      case e: IOException =>
        EventHandler notifyListeners EventHandler.Error(e, this, "Could not close AMQP connection")
      case _ => ()
    }
    connection = None
  }

  private def notifyCallback(message: AMQPMessage) = {
    connectionCallback.foreach(cb => if (cb.isRunning) cb ! message)
  }

  override def postStop = {
    reconnectionFuture.foreach(_.cancel(true))
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
