/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp

import rpc.RPC
import rpc.RPC.{RpcClientSerializer, RpcServerSerializer}
import akka.actor.{Actor}
import Actor._
import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.lang.String
import akka.amqp.AMQP._
import akka.amqp.AkkaAmqp.TestMessage

object ExampleSession {

  def main(args: Array[String]) = {

    printTopic("DIRECT")
    direct

    printTopic("FANOUT")
    fanout

    printTopic("TOPIC")
    topic

    printTopic("CALLBACK")
    callback

    printTopic("EASY STRING PRODUCER AND CONSUMER")
    easyStringProducerConsumer

    printTopic("EASY PROTOBUF PRODUCER AND CONSUMER")
    easyProtobufProducerConsumer

    printTopic("RPC")
    rpc

    printTopic("EASY STRING RPC")
    easyStringRpc

    printTopic("EASY PROTOBUF RPC")
    easyProtobufRpc

    printTopic("Happy hAkking :-)")

    // postStop everything the amqp tree except the main AMQP supervisor
    // all connections/consumers/producers will be stopped
    AMQP.shutdownAll

    Actor.registry.shutdownAll
    System.exit(0)
  }

  def printTopic(topic: String) {

    println("")
    println("==== " + topic + " ===")
    println("")
    TimeUnit.SECONDS.sleep(2)
  }

  def direct = {

    // defaults to amqp://guest:guest@localhost:5672/
    val connection = AMQP.newConnection()

    val exchangeParameters = ExchangeParameters("my_direct_exchange", Direct)

    val consumer = AMQP.newConsumer(connection, ConsumerParameters("some.routing", actorOf(new Actor { def receive = {
      case Delivery(payload, _, _, _, _, _) => println("@george_bush received message from: %s".format(new String(payload)))
    }}), None, Some(exchangeParameters)))

    val producer = AMQP.newProducer(connection, ProducerParameters(Some(exchangeParameters)))
    producer ! Message("@jonas_boner: You sucked!!".getBytes, "some.routing")
  }

  def fanout = {

    // defaults to amqp://guest:guest@localhost:5672/
    val connection = AMQP.newConnection()

    val exchangeParameters = ExchangeParameters("my_fanout_exchange", Fanout)

    val bushConsumer = AMQP.newConsumer(connection, ConsumerParameters("@george_bush", actorOf(new Actor { def receive = {
      case Delivery(payload, _, _, _, _, _) => println("@george_bush received message from: %s".format(new String(payload)))
    }}), None, Some(exchangeParameters)))

    val obamaConsumer = AMQP.newConsumer(connection, ConsumerParameters("@barack_obama", actorOf(new Actor { def receive = {
      case Delivery(payload, _, _, _, _, _) => println("@barack_obama received message from: %s".format(new String(payload)))
    }}), None, Some(exchangeParameters)))

    val producer = AMQP.newProducer(connection, ProducerParameters(Some(exchangeParameters)))
    producer ! Message("@jonas_boner: I'm going surfing".getBytes, "")
  }

  def topic = {

    // defaults to amqp://guest:guest@localhost:5672/
    val connection = AMQP.newConnection()

    val exchangeParameters = ExchangeParameters("my_topic_exchange", Topic)

    val bushConsumer = AMQP.newConsumer(connection, ConsumerParameters("@george_bush", actorOf(new Actor { def receive = {
      case Delivery(payload, _, _, _, _, _) => println("@george_bush received message from: %s".format(new String(payload)))
    }}), None, Some(exchangeParameters)))

    val obamaConsumer = AMQP.newConsumer(connection, ConsumerParameters("@barack_obama", actorOf(new Actor { def receive = {
      case Delivery(payload, _, _, _, _, _) => println("@barack_obama received message from: %s".format(new String(payload)))
    }}), None, Some(exchangeParameters)))

    val producer = AMQP.newProducer(connection, ProducerParameters(Some(exchangeParameters)))
    producer ! Message("@jonas_boner: You still suck!!".getBytes, "@george_bush")
    producer ! Message("@jonas_boner: Yes I can!".getBytes, "@barack_obama")
  }

  def callback = {

    val channelCountdown = new CountDownLatch(2)

    val connectionCallback = actorOf(new Actor { def receive = {
      case Connected => println("Connection callback: Connected!")
      case Reconnecting => () // not used, sent when connection fails and initiates a reconnect
      case Disconnected => println("Connection callback: Disconnected!")
    }})
    val connection = AMQP.newConnection(new ConnectionParameters(connectionCallback = Some(connectionCallback)))

    val channelCallback = actorOf(new Actor { def receive = {
      case Started => {
        println("Channel callback: Started")
        channelCountdown.countDown
      }
      case Restarting => // not used, sent when channel or connection fails and initiates a restart
      case Stopped => println("Channel callback: Stopped")
    }})
    val exchangeParameters = ExchangeParameters("my_callback_exchange", Direct)
    val channelParameters = ChannelParameters(channelCallback = Some(channelCallback))

    val consumer = AMQP.newConsumer(connection, ConsumerParameters("callback.routing", actorOf(new Actor { def receive = {
      case _ => () // not used
    }}), None, Some(exchangeParameters), channelParameters = Some(channelParameters)))

    val producer = AMQP.newProducer(connection, ProducerParameters(Some(exchangeParameters)))

    // Wait until both channels (producer & consumer) are started before stopping the connection
    channelCountdown.await(2, TimeUnit.SECONDS)
    connection.stop
  }

  def easyStringProducerConsumer = {
    val connection = AMQP.newConnection()

    val exchangeName = "easy.string"

    // listen by default to:
    // exchange = optional exchangeName
    // routingKey = provided routingKey or <exchangeName>.request
    // queueName = <routingKey>.in
    AMQP.newStringConsumer(connection, message => println("Received message: "+message), Some(exchangeName))

    // send by default to:
    // exchange = exchangeName
    // routingKey = <exchange>.request
    val producer = AMQP.newStringProducer(connection, Some(exchangeName))

    producer.send("This shit is easy!")
  }

  def easyProtobufProducerConsumer = {
    val connection = AMQP.newConnection()

    val exchangeName = "easy.protobuf"

    def protobufMessageHandler(message: TestMessage) = {
      println("Received "+message)
    }

    AMQP.newProtobufConsumer(connection, protobufMessageHandler _, Some(exchangeName))

    val producerClient = AMQP.newProtobufProducer[TestMessage](connection, Some(exchangeName))
    producerClient.send(TestMessage.newBuilder.setMessage("akka-amqp rocks!").build)
  }

  def rpc = {

    val connection = AMQP.newConnection()

    val exchangeName = "my_rpc_exchange"

    /** Server */
    val serverFromBinary = new FromBinary[String] {
      def fromBinary(bytes: Array[Byte]) = new String(bytes)
    }
    val serverToBinary = new ToBinary[Int] {
      def toBinary(t: Int) = Array(t.toByte)
    }
    val rpcServerSerializer = new RpcServerSerializer[String, Int](serverFromBinary, serverToBinary)

    def requestHandler(request: String) = 3

    val rpcServer = RPC.newRpcServer(connection, exchangeName, rpcServerSerializer, requestHandler _,
      routingKey = Some("rpc.in.key"), queueName = Some("rpc.in.key.queue"))


    /** Client */
    val clientToBinary = new ToBinary[String] {
      def toBinary(t: String) = t.getBytes
    }
    val clientFromBinary = new FromBinary[Int] {
      def fromBinary(bytes: Array[Byte]) = bytes.head.toInt
    }
    val rpcClientSerializer = new RpcClientSerializer[String, Int](clientToBinary, clientFromBinary)

    val rpcClient = RPC.newRpcClient(connection, exchangeName, rpcClientSerializer, Some("rpc.in.key"))

    val response = rpcClient.call("rpc_request")
    println("Response: " + response)
  }

  def easyStringRpc = {

    val connection = AMQP.newConnection()

    val exchangeName = "easy.stringrpc"

    // listen by default to:
    // exchange = exchangeName
    // routingKey = <exchange>.request
    // queueName = <routingKey>.in
    RPC.newStringRpcServer(connection, exchangeName, request => {
      println("Got request: "+request)
      "Response to: '"+request+"'"
    })

    // send by default to:
    // exchange = exchangeName
    // routingKey = <exchange>.request
    val stringRpcClient = RPC.newStringRpcClient(connection, exchangeName)

    val response = stringRpcClient.call("AMQP Rocks!")
    println("Got response: "+response)

    stringRpcClient.callAsync("AMQP is dead easy") {
      case response => println("This is handled async: "+response)
    }
  }

  def easyProtobufRpc = {

    val connection = AMQP.newConnection()

    val exchangeName = "easy.protobuf.rpc"

    def protobufRequestHandler(request: TestMessage): TestMessage = {
      TestMessage.newBuilder.setMessage(request.getMessage.reverse).build
    }

    RPC.newProtobufRpcServer(connection, exchangeName, protobufRequestHandler)

    val protobufRpcClient = RPC.newProtobufRpcClient[TestMessage, TestMessage](connection, exchangeName)

    val response = protobufRpcClient.call(TestMessage.newBuilder.setMessage("akka-amqp rocks!").build)

    println("Got response: "+response)
  }
}
