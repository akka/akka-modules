OSGi Support
============

Akka currently provides a certain kind of OSGi support which we call **//OSGi enabled//**. What does that mean?

First, all the Akka modules are **OSGi bundles**, i.e. they have got OSGi headers like *Bundle-SymbolicName*, *Bundle-Version*, *Import-Package* etc. in the manifest file (*META-INF/MANIFEST.MF*) of the the respective JAR archives.

Second, all necessary dependencies that are not already OSGi bundles (e.g. *commons-io-1.4.jar*) are wrapped into a **big dependencies bundle** exporting all their packages. While this is not the most modular approach, it is an easy path towards running Akka inside an OSGi container. This dependencies bundle is the artifact of the *akka-osgi-dependencies-bundle* module which itself is a subproject of the *akka-osgi* module.

Third, the *akka-osgi-assembly* module which also is a subproject of the *akka-osgi* module will **assemble everything you need** to run Akka inside an OSGi container. In its *target/<scala-version>/bundles* directory you will find the Akka bundles and all dependency bundles.

Last but not least, there is a simple OSGi example for Akka Core in the *akka-sample-osgi* module which itself is a subproject of the *akka-samples* module. It will start an *EchoActor* and send a message to it on bundle activation and shut it down on bundle deactivation. In order to run this example all you have to run an OSGi container like Eclipse Equinox or Apache Felix and install all the above mentioned bundles as well as this example bundle. An easy way to achieve this is using `Pax Runner <@http://paxrunner.ops4j.org/space/Pax+Runner>`_, step into the *target/<scala-version>/bundles* directory of the *akka-osgi-assembly* module and enter the following on the console:

::

  pax-run.sh --p=equinox --profiles=log scan-dir:.@update file:../../../../../akka-samples/akka-sample-osgi/target/scala_2.8.0/akka-sample-osgi_2.8.0-0.10.jar
