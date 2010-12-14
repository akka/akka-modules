package akka.persistence.terrastore

import akka.util.Logging
import org.scalatest.{Suite, BeforeAndAfterAll}
import terrastore.test.embedded.TerrastoreEmbeddedServer

trait EmbeddedTerrastore extends BeforeAndAfterAll with Logging {
  this: Suite =>

  var server: TerrastoreEmbeddedServer = _

  override protected def beforeAll(): Unit = {
    server = new TerrastoreEmbeddedServer
  }

  override protected def afterAll(): Unit = {
    server.stop
  }
}