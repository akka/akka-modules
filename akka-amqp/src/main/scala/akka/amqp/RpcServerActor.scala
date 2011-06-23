/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp

import rpc.RPC.RpcServerSerializer
import akka.actor.{ActorRef, Actor}
import akka.event.EventHandler

import com.rabbitmq.client.AMQP.BasicProperties

class RpcServerActor[I,O](
    producer: ActorRef,
    serializer: RpcServerSerializer[I,O],
    requestHandler: I => O) extends Actor {

  protected def receive = {
    case Delivery(payload, _, tag, _, props, sender) => {

      val request = serializer.fromBinary.fromBinary(payload)
      val response: Array[Byte] =  serializer.toBinary.toBinary(requestHandler(request))



      val replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId).build()
      producer ! new Message(response, props.getReplyTo, properties = Some(replyProps))

      sender.foreach(_ ! Acknowledge(tag))
    }
    case Acknowledged(tag) => EventHandler notifyListeners EventHandler.Debug(this, "%s acknowledged delivery with tag %d".format(this, tag))
  }

  override def toString = "AMQP.RpcServer[]"
}
