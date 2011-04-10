Persistence
===========

**The persistence modules have been deprecated.**

The documentation for the 1.0 release of the persistence modules can be found `here <http://akka.io/docs/akka-1.0/persistence.html>`_.

Below is the reasoning behind this removal. This was announced on the Akka mailing list `here <http://groups.google.com/group/akka-user/browse_thread/thread/8867fd5c9d64db4e>`_.

Reasoning behind removal of Akka Persistence modules
====================================================

Dear hakkers.

We have after much thinking, discussions and analysis decided to drop support for the Akka Persistence Modules. It is part of Akka 1.0, but deprecated and will be removed for Akka 1.1 where we will instead release Akka Persistence Modules 1.1 as a separate release for the people needing it. It will also be moved out of the Akka Modules repository and not maintained by the Akka Team any longer. This is a pretty radical decision and might come as a surprise to most people. We hope to not offend or disrespect anyone of all the great developers that have been contributing code and ideas to these modules. But we strongly believe that it is the right thing to do and will be a good thing for Akka going forward. Please take the time to read the email in its entirety. We will try to explain why and the reasoning behind the decision.

Background
----------

When Akka Persistence modules(s) was started it was intended as a durable storage for STM, combining the two would give the user ACID, ACI from STM and D from the underlying storage solution. It was an experiment starting with a couple of databases which have now grown into a large set of modules supporting many different databases, all with different semantics and guarantees for ACIDness. In theory it might sound like a good idea to have a single transaction layer on top of so many different databases.

The problem
-----------

However, we have discovered that it gives an illusion of safety that is different for each storage and we simply cannot guarantee that it’s crash-proof. There have been many issues with both which guarantees it can give and which semantics it has in different situations. It has proven to be almost impossible to build a single abstraction layer that deals with all these different models for ACID.

Secondly, people are using the persistence modules as an interface to NoSQL storage, without needing the STM. This can be confusing and easily lead to a misunderstanding of what is possible and the original intentions.

Detailed analysis
-----------------

No failure atomicity:
^^^^^^^^^^^^^^^^^^^^^

Placement of the items on the nosql store is not atomic. So it could be that when a transaction fails (for whatever reason) some of the nosql puts are already executed. The abort of the stm transaction is not going to roll these changes back. This problem is increased even more by the optimistic nature of STM (and even more with the speculative behavior if enabled) which can lead to multiple retries (that is why there is a max retry limit since we know that a transaction can be retried). Part of these problems can be solved by putting the stm in a non consistent (read committed isolation level) in combination with a lock on all reads (can be a less expensive read lock). But locking reduces concurrency, causes more overhead and the with the reduced isolation level, the added value of stm is questionable.

No consistency:
^^^^^^^^^^^^^^^

STM out of the box provides a very high level of read consistency (by default the snapshot isolation level is provided and is exactly the same as the Oracle version of Serialized). Example, if there are 100 refs all initialized with the value 0 and there is an update transaction that increases all refs atomically and there are reading transactions, they will always see exactly the same value for all references (whatever the value may be). With NoSQL there doesn’t exist such a high level of read consistency since their designs go in the exact opposite direction (eventual consistency). So the consistency models of STM and NoSQL are extremes. It is possible to put the STM in a lower consistency mode (e.g. read committed/repeatable read), but that make STM much much harder to reason about (race problems).

Another big difference is that write consistency is dead simple with STM, but with NoSQL this can even be something of a challenge. So even the write consistency models differ and can cause subtle problems if not dealt with correctly.

No isolation:
^^^^^^^^^^^^^

The STM provides isolation in memory for a specific JVM. But if the same nosql ‘record’ is used by multiple machines, the STM is not going to be able to provide any isolation. Even locks acquired on one machine on STM level will not translate in locks on the NoSQL store. To get this to work, you need something like distributed STM.

Even on a single machine there could be issues of the same NoSQL records are accessed by any other means than the STM.

Lost updates:
^^^^^^^^^^^^^

If there are multiple machines changing the data, there is no optimistic locking on writing the data. So if data of version X is read, another transaction changes it to X+1 and the first transaction commits, the resulting version is X+1 and no optimistic locking conflict is thrown. If you use a or mapper like hibernate for example and activate optimistic locking, it will detect and abort the transaction. In our case, the lost update is not detected and the essential leads to a permanent inconsistent state since it will not be repaired (afaik) by a developer.
I know that Brendan McAdams (of Mongo) is working on some functionality, we both figured out that some essential operations (like acquiring a lock) was not going to make it in Mongo.

Possible future solutions:
^^^^^^^^^^^^^^^^^^^^^^^^^^

To combine STM and distribution/storage, you need to have a solution where the approaches are compatible. E.g. if you look at Terracotta or Gigaspaces, they provide the basic operations to satisfy the guarantees needed for Distributed STM. Failure atomicity (transactions), various levels of locking, isolation etc.

Thanks for your understanding,

Jonas Bonér
Viktor Klang
Peter Veentjer
Peter Vlugter
