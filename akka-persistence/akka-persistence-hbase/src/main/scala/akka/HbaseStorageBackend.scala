/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.persistence.hbase

import scala.collection.Map
import scala.collection.immutable.Iterable
import scala.collection.mutable.HashMap
import akka.persistence.common._
import akka.config.Config.config
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.util.Bytes


/**
 * @author <a href="http://www.davidgreco.it">David Greco</a>
 */
private[akka] object HbaseStorageBackend extends CommonStorageBackend {

  val HBASE_ZOOKEEPER_QUORUM = config.getString("akka.persistence.hbase.zookeeper-quorum", "localhost")
  val CONFIGURATION = new HBaseConfiguration
  val REF_TABLE_NAME = "__REF_TABLE"
  val VECTOR_TABLE_NAME = "__VECTOR_TABLE"
  val VECTOR_ELEMENT_COLUMN_FAMILY_NAME = "__VECTOR_ELEMENT"
  val MAP_ELEMENT_COLUMN_FAMILY_NAME = "__MAP_ELEMENT"
  val MAP_TABLE_NAME = "__MAP_TABLE"
  var ADMIN: HBaseAdmin = _

  CONFIGURATION.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER_QUORUM)
  ADMIN = new HBaseAdmin(CONFIGURATION)

  class HbaseAccess(val store: String) extends CommonStorageBackendAccess {

    if (!ADMIN.tableExists(store)) {
      ADMIN.createTable(new HTableDescriptor(store))
      ADMIN.disableTable(store)
      ADMIN.addColumn(store, new HColumnDescriptor(store))
      ADMIN.enableTable(store)
    }
    val table = new HTable(CONFIGURATION, store);

    def put(owner: String, key: Array[Byte], value: Array[Byte]) = {
      val row  = new Put(Bytes.toBytes(owner))
      row.add(Bytes.toBytes(store), key, value)
      table.put(row)
    }

    override def get(owner: String, key: Array[Byte]): Array[Byte] = {
      val row = new Get(Bytes.toBytes(owner))
      val result = table.get(row)
      result.getValue(Bytes.toBytes(store), key)
    }

    def get(owner: String, key: Array[Byte], default: Array[Byte]): Array[Byte] = {
      val row = new Get(Bytes.toBytes(owner))
      val result = table.get(row)
      if (result.isEmpty())
        default
      else  {
        val r = result.getValue(Bytes.toBytes(store), key)
        if(r == null)
          default
        else
          r
      }
    }

    def getAll(keys: Iterable[Array[Byte]]): Map[Array[Byte], Array[Byte]] = {
      keys.foreach( k => {println(k)})
      new HashMap[Array[Byte], Array[Byte]]
    }

    def delete(owner: String, key: Array[Byte]) = {
      val delrow = new Delete(Bytes.toBytes(owner))
      delrow.deleteColumns(Bytes.toBytes(store), key)
      table.delete(delrow)
    }

    def drop() = {
      ADMIN.disableTable(store)
      ADMIN.deleteTable(store)
    }
  }

  def queueAccess = new HbaseAccess("QUEUE")

  def mapAccess = new HbaseAccess("MAP")

  def vectorAccess = new HbaseAccess("VECTOR")

  def refAccess = new HbaseAccess("REF")

}
