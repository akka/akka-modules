/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.amqp.test

import akka.amqp.AMQP

object AMQPTest {

  def withCleanEndState(action: => Unit) {
    try {
      try {
        action
      } finally {
        AMQP.shutdownAll
      }
    } catch {
      case e => println(e)
    }
  }
}
