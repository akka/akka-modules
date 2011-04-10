Microkernel
===========

=

Module stability: **SOLID**

Download the full Akka Modules distribution from `http://akka.io/downloads <http://akka.io/downloads>`_

Run the microkernel
===================

* To start the kernel invoke ‘java -jar $AKKA_HOME/akka-modules-<version>.jar’.
* Wait for all services to start up.
* Done.

All services are configured in the ‘$AKKA_HOME/config/akka.conf’ configuration file. See the reference section on the configuration file for details.
Services you want to be started up automatically should be listed in the list of "boot" classes. See the `config section <configuration>`_ for details.

The kernel need to find the 'akka.conf' file. You can specify the config by either:
* Specifying 'AKKA_HOME'. Then the config will be taken from 'AKKA_HOME/config/akka.conf'.
* Specifying the '-Dakka.config.file=./config/akka.conf' option.
* Specifying the '-Dakka.home' option
* Putting it on the classpath
