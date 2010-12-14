package akka.persistence.terrastore

import akka.persistence.common.{BytesStorage, Backend}

/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

object TerrastoreStorage extends BytesStorage {
  val backend = TerrastoreBackend
}

object TerrastoreBackend extends Backend[Array[Byte]] {
  val sortedSetStorage = None
  val refStorage = Some(TerrastoreStorageBackend)
  val vectorStorage = Some(TerrastoreStorageBackend)
  val queueStorage = Some(TerrastoreStorageBackend)
  val mapStorage = Some(TerrastoreStorageBackend)
}
