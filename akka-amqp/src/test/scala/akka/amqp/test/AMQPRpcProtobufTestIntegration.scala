package akka.amqp.test

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */
import org.scalatest.matchers.MustMatchers
import org.scalatest.junit.JUnitSuite
import akka.amqp.AMQP
import org.junit.Test
import akka.amqp.rpc.RPC
import org.multiverse.api.latches.StandardLatch
import java.util.concurrent.TimeUnit
import akka.amqp.AkkaAmqp.TestMessage

class AMQPRpcProtobufTestIntegration extends JUnitSuite with MustMatchers {

  @Test
  def consumerMessage = AMQPTest.withCleanEndState {

    val connection = AMQP.newConnection()

    RPC.newProtobufRpcServer(connection, "protoservice", requestHandler)

    val protobufClient = RPC.newProtobufRpcClient[TestMessage, TestMessage](connection, "protoservice")

    val request = TestMessage.newBuilder.setMessage("testmessage").build

    protobufClient.call(request) match {
      case Some(response) => assert(response.getMessage == request.getMessage.reverse)
      case None => fail("no response")
    }

    val aSyncLatch = new StandardLatch
    protobufClient.callAsync(request) {
      case Some(response) => {
        assert(response.getMessage == request.getMessage.reverse)
        aSyncLatch.open
      }
      case None => fail("no response")
    }

    aSyncLatch.tryAwait(2, TimeUnit.SECONDS) must be (true)

  }

  def requestHandler(request: TestMessage): TestMessage = {
    TestMessage.newBuilder.setMessage(request.getMessage.reverse).build
  }
}
