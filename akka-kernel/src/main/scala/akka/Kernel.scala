/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.kernel

import akka.http.{ EmbeddedAppServer, DefaultAkkaLoader }
import akka.remote.BootableRemoteActorService

import java.util.concurrent.CountDownLatch

object Main {
  val keepAlive = new CountDownLatch(2)
 
  def main(args: Array[String]) = {
    Kernel.boot
    keepAlive.await
  }
}

/**
 * The Akka Kernel, is used to start And postStop Akka in standalone/kernel mode.
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
object Kernel extends DefaultAkkaLoader {
  
    //For testing purposes only
  def startRemoteService(): Unit = bundles.foreach( _ match {
    case x: BootableRemoteActorService => x.startRemoteService
    case _ =>
  })
}
