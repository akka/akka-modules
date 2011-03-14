package akka.amqp.test

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitSuite
import akka.amqp.AMQP
import org.junit.Test
import org.multiverse.api.latches.StandardLatch
import java.util.concurrent.TimeUnit
import akka.amqp.rpc.RPC
import akka.amqp.AkkaAmqp.TestMessage

class AMQPProtobufProducerConsumerTestIntegration extends JUnitSuite with MustMatchers {

  @Test
  def consumerMessage = AMQPTest.withCleanEndState {

    val connection = AMQP.newConnection()

    val responseLatch = new StandardLatch

    RPC.newProtobufRpcServer(connection, "protoexchange", requestHandler)

    val request = TestMessage.newBuilder.setMessage("testmessage").build

    def responseHandler(response: TestMessage) = {
      assert(response.getMessage == request.getMessage.reverse)
      responseLatch.open
    }
    AMQP.newProtobufConsumer(connection, responseHandler _, None, Some("proto.reply.key"))

    val producer = AMQP.newProtobufProducer[TestMessage](connection, Some("protoexchange"))
    producer.send(request, Some("proto.reply.key"))

    responseLatch.tryAwait(2, TimeUnit.SECONDS) must be (true)
  }

  def requestHandler(request: TestMessage): TestMessage = {
    TestMessage.newBuilder.setMessage(request.getMessage.reverse).build
  }
}
