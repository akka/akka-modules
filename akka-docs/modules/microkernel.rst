
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

To start the kernel use the scripts ``start.sh`` or ``start.bat``.

All services are configured in the ``config/akka.conf`` configuration file. See
the Akka documentation on Configuration for more details. Services you want to
be started up automatically should be listed in the list of ``boot`` classes in
the configuration.

Put your application in the ``deploy`` directory.

Note that the kernel needs to know where the Akka home is (the base directory of
the microkernel). The above scripts do this for you. Otherwise, you can set Akka
home by:

* Specifying ``AKKA_HOME``. Then the config will be taken from
  ``AKKA_HOME/config/akka.conf``

* Specifying the ``-Dakka.home`` option
