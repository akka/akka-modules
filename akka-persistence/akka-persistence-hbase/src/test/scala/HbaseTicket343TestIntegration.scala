package  akka.persistence.hbase

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.persistence.common._

@RunWith(classOf[JUnitRunner])
class HbaseTicket343Test extends Ticket343Test with EmbeddedHbase {
  def dropMapsAndVectors: Unit = {
    HbaseStorageBackend.vectorAccess.drop
    HbaseStorageBackend.mapAccess.drop
  }

  def getVector: (String) => PersistentVector[Array[Byte]] = HbaseStorage.getVector

  def getMap: (String) => PersistentMap[Array[Byte], Array[Byte]] = HbaseStorage.getMap

}