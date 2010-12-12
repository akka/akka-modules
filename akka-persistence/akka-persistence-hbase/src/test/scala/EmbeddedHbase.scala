package akka.persistence.hbase

import org.apache.hadoop.hbase.HBaseTestingUtility
import akka.util.Logging
import org.scalatest.{Suite, BeforeAndAfterAll}

trait EmbeddedHbase extends BeforeAndAfterAll with Logging {
  this: Suite =>

  val testUtil = new HBaseTestingUtility

  override protected def beforeAll(): Unit = {
    testUtil.startMiniCluster
  }

  override protected def afterAll(): Unit = {
    testUtil.shutdownMiniCluster
  }
}
