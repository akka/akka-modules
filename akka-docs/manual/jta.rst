JTA integration (Scala)
=======================

Module stability: **STABLE**

Integrating JTA with Akka STM
-----------------------------

The STM has JTA (Java Transaction API) integration. This means that it will, if enabled, hook in to JTA and start a JTA transaction when the STM transaction is started. It will also rollback the STM transaction if the JTA transaction has failed and vice versa. This does not mean that the STM is made durable, if you need that you should use one of the `persistence modules <persistence>`_. It simply means that the STM will participate and interact with and external JTA provider, for example send a message using JMS atomically within an STM transaction, or use Hibernate to persist STM managed data etc.

You can enable JTA support in the 'stm' section in the config:

.. code-block:: ruby

    ...
    stm {
      ...
      jta-aware = off          # 'on' means that if there JTA Transaction Manager available then the STM will
                               # begin (or join), commit or rollback the JTA transaction. Default is 'off'.
    }
    ...

You also have to configure which JTA provider to use etc in the 'jta' config section:

.. code-block:: ruby

    ...
    jta {
      provider = "from-jndi"   # Options: "from-jndi" (means that Akka will try to detect a TransactionManager in the JNDI)
                               #          "atomikos" (means that Akka will use the Atomikos based JTA impl in 'akka-jta',
                               #          e.g. you need the akka-jta JARs on classpath).
      timeout = 60000
    }
    ...

API for JTA management
----------------------

The semantics are the same as used in the EJB spec. E.g. Required, RequiresNew, Mandatory, Supports, Never. All these are exposed as monadic objects and high-order functions in the TransactionContext object.

There are two versions of the API, one monadic and one using higher-order functions.

Monadic API
^^^^^^^^^^^

Using for comprehension to produce a side-effect.

.. code-block:: scala

   for {
     ctx <- TransactionContext.Required
     entity <- updatedEntities
     if !ctx.isRollbackOnly
   } {
     // transactional stuff
     ...
   }

Using for comprehension with yield to yield a result of the computation.

.. code-block:: scala

  val users = for {
     ctx <- TransactionContext.RequiresNew
     name <- userNames
   } yield {
     // transactional stuff
     ... // grab the user
     user
   }

Higher-order function API
^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: scala

  import TransactionContext._

  withTxRequired {
      ... // REQUIRED semantics

    withTxRequiresNew {
      ... // REQUIRES_NEW semantics
    }
  }

Hook in JPA and other transactional backends
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to for example hook in JPA's EntityManager and have JTA manage its transactions then you have to register a 'Synchronization' instance in the 'TransactionContext.registerSynchronization' method. You most likely also have to utilize the 'TransactionContext.registerJoinTransactionFun' and 'TransactionContext.registerExceptionNotToRollbackOn' methods.

Here are some examples on how to add a JPA EntityManager integration.

Register Synchronization instance
*********************************

.. code-block:: scala

     TransactionContext.registerSynchronization(new javax.transaction.Synchronization() {
       val em: EntityManager = ... // get the EntityManager
       def beforeCompletion = {
         try {
           val status = tm.getStatus
           if (status != Status.STATUS_ROLLEDBACK &&
               status != Status.STATUS_ROLLING_BACK &&
               status != Status.STATUS_MARKED_ROLLBACK) {
             log.debug("Flushing EntityManager...")
             em.flush // flush EntityManager on success
           }
         } catch {
           case e: javax.transaction.SystemException => throw new RuntimeException(e)
         }
       }

       def afterCompletion(status: Int) = {
         val status = tm.getStatus
        if (closeAtTxCompletion) em.close
         if (status == Status.STATUS_ROLLEDBACK ||
            status == Status.STATUS_ROLLING_BACK ||
             status == Status.STATUS_MARKED_ROLLBACK) {
           em.close
         }
       }
     })

Register function to join transaction
*************************************

.. code-block:: scala

  TransactionContext.registerJoinTransactionFun(() => {
    val em: EntityManager = ... // get the EntityManager
    em.joinTransaction // join JTA transaction
  })

Register classes for exceptions not to roll back on
***************************************************

.. code-block:: scala

  TransactionContext.registerExceptionNotToRollbackOn(classOf[NoResultException])
  TransactionContext.registerExceptionNotToRollbackOn(classOf[NonUniqueResultException])

Example: Integrating with Hibernate JPA
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Apart from registering the different things detailed above, in order to integrate with Hibernate, you also have to write a ‘TransactionManagerLookup’ class and add that to you ‘persistence.xml’ file.

Here is an example:

.. code-block:: scala

  class MyHibernateTransactionManagerLookup extends org.hibernate.transaction.TransactionManagerLookup {
    def getTransactionManager(props: _root_.java.util.Properties): TransactionManager = TransactionContext.tm match {
      case Right(Some(tm)) => tm
      case _ => throw new Exception(“Can’t retrieve TransactionManager”)
    }

    def getUserTransactionName: String = "java:comp/UserTransaction"

    def getTransactionIdentifier(tx: Transaction) = tx
  }

The configuration is done in the persistence.xml file + the jta.properties. Sample configuration files can be found in the src/main/resources directory.
Here are the essential configuration options in the JPA persistence.xml file:

`<code format="xml">`_
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http:*java.sun.com/xml/ns/persistence http:*java.sun.com/xml/ns/persistence/persistence_1_0.xsd"
             version="1.0">
  <persistence-unit name="LiftPersistenceUnit" transaction-type="JTA">
    <provider>org.hibernate.ejb.HibernatePersistence</provider>

    <mapping-file>...</mapping-file>
    <class>...</class>

    <properties>
      <property name="hibernate.transaction.manager_lookup_class"
                value="[...].MyHibernateTransactionManagerLookup" />
    </properties>
  </persistence-unit>
</persistence>
</pre>
`<code>`_
