package akka.persistence.terrastore

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.persistence.common._

@RunWith(classOf[JUnitRunner])
class TerrastoreTicket343Test extends Ticket343Test with EmbeddedTerrastore {
  def dropMapsAndVectors: Unit = {
    TerrastoreStorageBackend.vectorAccess.drop
    TerrastoreStorageBackend.mapAccess.drop
  }

  def getVector: (String) => PersistentVector[Array[Byte]] = TerrastoreStorage.getVector

  def getMap: (String) => PersistentMap[Array[Byte], Array[Byte]] = TerrastoreStorage.getMap

}