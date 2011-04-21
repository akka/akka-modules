
.. _microkernel:

#############
 Microkernel
#############


Download Akka Modules
=====================

Download the full Akka Modules distribution from http://akka.io/downloads


Build latest version from source
================================

To build the latest version see :ref:`building-akka-modules`.


Run the microkernel
===================

To start the kernel invoke::

   java -jar $AKKA_HOME/akka-modules-<version>.jar


All services are configured in the ``$AKKA_HOME/config/akka.conf`` configuration
file. See the Akka documentation on Configuration for more details.

Services you want to be started up automatically should be listed in the list of
"boot" classes.

The kernel needs to find the ``akka.conf`` file. You can specify the config by
either:

* Specifying ``AKKA_HOME``. Then the config will be taken from
  ``AKKA_HOME/config/akka.conf``

* Specifying the ``-Dakka.config.file=./config/akka.conf`` option

* Specifying the ``-Dakka.home`` option

* Putting it on the classpath
