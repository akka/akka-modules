package akka.persistence.terrastore

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.persistence.common.{QueueStorageBackendTest, VectorStorageBackendTest, MapStorageBackendTest, RefStorageBackendTest}

@RunWith(classOf[JUnitRunner])
class TerrastoreRefStorageBackendTest extends RefStorageBackendTest with EmbeddedTerrastore {
  def dropRefs = {
    TerrastoreStorageBackend.refAccess.drop
  }

  def storage = TerrastoreStorageBackend
}

@RunWith(classOf[JUnitRunner])
class TerrastoreMapStorageBackendTest extends MapStorageBackendTest with EmbeddedTerrastore {
  def dropMaps = {
    TerrastoreStorageBackend.mapAccess.drop
  }

  def storage = TerrastoreStorageBackend
}

@RunWith(classOf[JUnitRunner])
class TerrastoreVectorStorageBackendTest extends VectorStorageBackendTest with EmbeddedTerrastore {
  def dropVectors = {
    TerrastoreStorageBackend.vectorAccess.drop
  }

  def storage = TerrastoreStorageBackend
}


@RunWith(classOf[JUnitRunner])
class TerrastoreQueueStorageBackendTest extends QueueStorageBackendTest with EmbeddedTerrastore {
  def dropQueues = {
    TerrastoreStorageBackend.queueAccess.drop
  }


  def storage = TerrastoreStorageBackend
}