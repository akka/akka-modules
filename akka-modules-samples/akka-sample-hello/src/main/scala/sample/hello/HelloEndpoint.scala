/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package sample.hello

import akka.actor._
import akka.http._

import java.text.DateFormat
import java.util.Date

class HelloEndpoint extends Actor with Endpoint {
  self.dispatcher = Endpoint.Dispatcher

  lazy val hello = Actor.actorOf(
    new Actor {
      def time = DateFormat.getTimeInstance.format(new Date)
      def receive = {
        case get: Get => get OK "Hello at " + time
      }
    }).start

  def hook(uri: String) = true

  def provide(uri: String) = hello

  override def preStart = Actor.registry.actorsFor(classOf[RootEndpoint]).head ! Endpoint.Attach(hook, provide)

  def receive = handleHttpRequest
}
