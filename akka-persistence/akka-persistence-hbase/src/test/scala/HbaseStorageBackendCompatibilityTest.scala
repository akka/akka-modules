package akka.persistence.hbase

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.persistence.common.{QueueStorageBackendTest, MapStorageBackendTest, VectorStorageBackendTest, RefStorageBackendTest}

@RunWith(classOf[JUnitRunner])
class HbaseRefStorageBackendTest extends RefStorageBackendTest with EmbeddedHbase {
  def dropRefs = {
    HbaseStorageBackend.refAccess.drop
  }

  def storage = HbaseStorageBackend
}

@RunWith(classOf[JUnitRunner])
class HbaseVectorStorageBackendTest extends VectorStorageBackendTest with EmbeddedHbase {
  def dropVectors = {
    HbaseStorageBackend.vectorAccess.drop
  }

  def storage = HbaseStorageBackend
}

@RunWith(classOf[JUnitRunner])
class HbaseMapStorageBackendTest extends MapStorageBackendTest with EmbeddedHbase {
  def dropMaps = {
    HbaseStorageBackend.mapAccess.drop
  }

  def storage = HbaseStorageBackend
}

@RunWith(classOf[JUnitRunner])
class HbaseQueueStorageBackendTest extends QueueStorageBackendTest with EmbeddedHbase {
  def dropQueues = {
    HbaseStorageBackend.queueAccess.drop
  }

  def storage = HbaseStorageBackend
}

