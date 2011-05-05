/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

import java.io.File
import java.util.jar.Attributes
import java.util.jar.Attributes.Name._
import sbt._
import sbt.CompileOrder._

// -------------------------------------------------------------------------------------------------------------------
// All repositories *must* go here! See ModuleConigurations below.
// -------------------------------------------------------------------------------------------------------------------

object Repositories {
  lazy val AkkaRepo               = MavenRepository("Akka Repository", "http://akka.io/repository")
  lazy val ScalaToolsRepo         = MavenRepository("Scala-Tools Repo", "http://scala-tools.org/repo-releases")
  lazy val CodehausRepo           = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
  lazy val LocalMavenRepo         = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
  lazy val GuiceyFruitRepo        = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
  lazy val JBossRepo              = MavenRepository("JBoss Repo", "http://repository.jboss.org/nexus/content/groups/public/")
  lazy val JavaNetRepo            = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
  lazy val SonatypeSnapshotRepo   = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
  lazy val SunJDMKRepo            = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
  lazy val ClojarsRepo            = MavenRepository("Clojars Repo", "http://clojars.org/repo")
}

class AkkaModulesParentProject(info: ProjectInfo) extends ParentProject(info) with DocParentProject { akkaModulesParent =>

  // -------------------------------------------------------------------------------------------------------------------
  // Compile settings
  // -------------------------------------------------------------------------------------------------------------------

  val scalaCompileSettings =
    Seq("-deprecation",
        "-Xmigration",
        "-optimise",
        "-encoding", "utf8")

  val javaCompileSettings = Seq("-Xlint:unchecked")

  // -------------------------------------------------------------------------------------------------------------------
  // Versions
  // -------------------------------------------------------------------------------------------------------------------

  lazy val AKKA_VERSION          = version.toString // matching versions
  lazy val HAWT_DISPATCH_VERSION = "1.1"
  lazy val CAMEL_VERSION         = "2.7.0"
  lazy val JACKSON_VERSION       = "1.7.1"
  lazy val JERSEY_VERSION        = "1.3"
  lazy val MULTIVERSE_VERSION    = "0.6.2"
  lazy val SCALATEST_VERSION     = "1.4.RC3"
  lazy val SPRING_VERSION        = "3.0.5.RELEASE"
  lazy val JETTY_VERSION         = "7.4.0.v20110414"
  lazy val CODEC_VERSION         = "1.4"
  lazy val LOGBACK_VERSION       = "0.9.28"

  // -------------------------------------------------------------------------------------------------------------------
  // ModuleConfigurations
  // Every dependency that cannot be resolved from the built-in repositories (Maven Central and Scala Tools Releases)
  // must be resolved from a ModuleConfiguration. This will result in a significant acceleration of the update action.
  // Therefore, if repositories are defined, this must happen as def, not as val.
  // -------------------------------------------------------------------------------------------------------------------

  import Repositories._

  lazy val jettyModuleConfig       = ModuleConfiguration("org.eclipse.jetty", sbt.DefaultMavenRepository)
  lazy val guiceyFruitModuleConfig = ModuleConfiguration("org.guiceyfruit", GuiceyFruitRepo)
  lazy val jbossModuleConfig       = ModuleConfiguration("org.jboss", JBossRepo)
  lazy val jdmkModuleConfig        = ModuleConfiguration("com.sun.jdmk", SunJDMKRepo)
  lazy val jmsModuleConfig         = ModuleConfiguration("javax.jms", SunJDMKRepo)
  lazy val jmxModuleConfig         = ModuleConfiguration("com.sun.jmx", SunJDMKRepo)
  lazy val jerseyContrModuleConfig = ModuleConfiguration("com.sun.jersey.contribs", JavaNetRepo)
  lazy val jerseyModuleConfig      = ModuleConfiguration("com.sun.jersey", JavaNetRepo)
  lazy val multiverseModuleConfig  = ModuleConfiguration("org.multiverse", CodehausRepo)
  lazy val nettyModuleConfig       = ModuleConfiguration("org.jboss.netty", JBossRepo)
  lazy val sjsonModuleConfig       = ModuleConfiguration("net.debasishg", ScalaToolsRepo)
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest", "scalatest", SCALATEST_VERSION, ScalaToolsRepo)
  lazy val atomikosModuleConfig    = ModuleConfiguration("com.atomikos",sbt.DefaultMavenRepository)
  lazy val commonsCodecModuleConfig= ModuleConfiguration("commons-codec", sbt.DefaultMavenRepository)
  lazy val timeModuleConfig        = ModuleConfiguration("org.scala-tools", "time", ScalaToolsRepo)
  lazy val args4jModuleConfig      = ModuleConfiguration("args4j", JBossRepo)
  lazy val scannotationModuleConfig= ModuleConfiguration("org.scannotation", JBossRepo)
  lazy val scalazModuleConfig      = ModuleConfiguration("org.scalaz", ScalaToolsSnapshots)
  lazy val scalacheckModuleConfig  = ModuleConfiguration("org.scala-tools.testing", "scalacheck_2.9.0.RC1", "1.9-SNAPSHOT", ScalaToolsSnapshots)
  lazy val aspectWerkzModuleConfig = ModuleConfiguration("org.codehaus.aspectwerkz", "aspectwerkz", "2.2.3", AkkaRepo)
  lazy val lzfModuleConfig         = ModuleConfiguration("voldemort.store.compress", "h2-lzf", AkkaRepo)
  lazy val rabbitModuleConfig      = ModuleConfiguration("com.rabbitmq","rabbitmq-client", "0.9.1", AkkaRepo)
  val localMavenRepo               = LocalMavenRepo // Second exception, also fast! ;-)

  // -------------------------------------------------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------------------------------------------------

  object Dependencies {

    // Compile

    lazy val activemq = "org.apache.activemq" % "activemq-core" % "5.4.2" % "compile" // ApacheV2

    lazy val akka_actor       = "se.scalablesolutions.akka" % "akka-actor"       % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_stm         = "se.scalablesolutions.akka" % "akka-stm"         % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_remote      = "se.scalablesolutions.akka" % "akka-remote"      % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_http        = "se.scalablesolutions.akka" % "akka-http"        % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_typed_actor = "se.scalablesolutions.akka" % "akka-typed-actor" % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_slf4j       = "se.scalablesolutions.akka" % "akka-slf4j"       % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_testkit     = "se.scalablesolutions.akka" % "akka-testkit"     % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_actor_tests = "se.scalablesolutions.akka" % "akka-actor-tests" % AKKA_VERSION % "compile" //ApacheV2

    lazy val aopalliance = "aopalliance" % "aopalliance" % "1.0" % "compile" //Public domain

    lazy val camel_core  = "org.apache.camel" % "camel-core"  % CAMEL_VERSION % "compile" //ApacheV2
    lazy val camel_jetty = "org.apache.camel" % "camel-jetty" % CAMEL_VERSION % "compile" //ApacheV2
    lazy val camel_jms   = "org.apache.camel" % "camel-jms"   % CAMEL_VERSION % "compile" //ApacheV2

    lazy val commons_codec = "commons-codec" % "commons-codec" % CODEC_VERSION % "compile" //ApacheV2

    lazy val commons_io = "commons-io" % "commons-io" % "1.4" % "compile" //ApacheV2

    lazy val commons_pool = "commons-pool" % "commons-pool" % "1.5.4" % "compile" //ApacheV2

    lazy val guicey = "org.guiceyfruit" % "guice-all" % "2.0" % "compile" //ApacheV2

    lazy val hawtdispatch = "org.fusesource.hawtdispatch" % "hawtdispatch-scala" % HAWT_DISPATCH_VERSION % "compile" //ApacheV2

    lazy val jackson          = "org.codehaus.jackson" % "jackson-mapper-asl" % JACKSON_VERSION % "compile" //ApacheV2
    lazy val jackson_core     = "org.codehaus.jackson" % "jackson-core-asl"   % JACKSON_VERSION % "compile" //ApacheV2

    lazy val jetty         = "org.eclipse.jetty" % "jetty-server"  % JETTY_VERSION % "compile" //Eclipse license
    lazy val jetty_util    = "org.eclipse.jetty" % "jetty-util"    % JETTY_VERSION % "compile" //Eclipse license
    lazy val jetty_xml     = "org.eclipse.jetty" % "jetty-xml"     % JETTY_VERSION % "compile" //Eclipse license
    lazy val jetty_servlet = "org.eclipse.jetty" % "jetty-servlet" % JETTY_VERSION % "compile" //Eclipse license

    lazy val jersey         = "com.sun.jersey"          % "jersey-core"   % JERSEY_VERSION % "compile" //CDDL v1
    lazy val jersey_json    = "com.sun.jersey"          % "jersey-json"   % JERSEY_VERSION % "compile" //CDDL v1
    lazy val jersey_server  = "com.sun.jersey"          % "jersey-server" % JERSEY_VERSION % "compile" //CDDL v1
    lazy val jersey_contrib = "com.sun.jersey.contribs" % "jersey-scala"  % JERSEY_VERSION % "compile" //CDDL v1
    lazy val stax_api       = "javax.xml.stream"        % "stax-api"      % "1.0-2"        % "compile" //ApacheV2

    lazy val jsr250 = "javax.annotation" % "jsr250-api" % "1.0" % "compile" //CDDL v1

    lazy val jsr311 = "javax.ws.rs" % "jsr311-api" % "1.1" % "compile" //CDDL v1

    lazy val jta_1_1 = "org.apache.geronimo.specs" % "geronimo-jta_1.1_spec" % "1.1.1" % "compile" intransitive //ApacheV2

    lazy val multiverse_test = "org.multiverse" % "multiverse-alpha" % MULTIVERSE_VERSION % "test" //ApacheV2

    lazy val netty = "org.jboss.netty" % "netty" % "3.2.3.Final" % "compile" //ApacheV2

    lazy val protobuf = "com.google.protobuf" % "protobuf-java" % "2.3.0" % "compile" //New BSD

    lazy val osgi_core = "org.osgi" % "org.osgi.core" % "4.2.0" //ApacheV2

    lazy val rabbit = "com.rabbitmq" % "amqp-client" % "2.3.1" % "compile" //Mozilla public license

    lazy val scalaz = "org.scalaz" % "scalaz-core_2.9.0.RC1" % "6.0-SNAPSHOT" % "compile" //New BSD

    lazy val spring_beans   = "org.springframework" % "spring-beans"   % SPRING_VERSION % "compile" //ApacheV2
    lazy val spring_context = "org.springframework" % "spring-context" % SPRING_VERSION % "compile" //ApacheV2
    lazy val spring_jms     = "org.springframework" % "spring-jms"     % SPRING_VERSION % "compile" //ApacheV2

    // Test

    lazy val testLogback    = "ch.qos.logback"         % "logback-classic"     % LOGBACK_VERSION   % "test" // EPL 1.0 / LGPL 2.1

    lazy val camel_spring   = "org.apache.camel"       % "camel-spring"        % CAMEL_VERSION     % "test" //ApacheV2
    lazy val commons_coll   = "commons-collections"    % "commons-collections" % "3.2.1"           % "test" //ApacheV2

    lazy val testJetty      = "org.eclipse.jetty"      % "jetty-server"        % JETTY_VERSION     % "test" //Eclipse license
    lazy val testJettyWebApp= "org.eclipse.jetty"      % "jetty-webapp"        % JETTY_VERSION     % "test" //Eclipse license

    lazy val junit          = "junit"                  % "junit"               % "4.5"             % "test" //Common Public License 1.0
    lazy val mockito        = "org.mockito"            % "mockito-all"         % "1.8.1"           % "test" //MIT
    lazy val scalatest      = "org.scalatest"          %% "scalatest"          % SCALATEST_VERSION % "test" //ApacheV2

    lazy val scalaz_scalacheck = "org.scalaz" % "scalaz-scalacheck-binding_2.9.0.RC1" % "6.0-SNAPSHOT" % "test" //New BSD
    lazy val scalacheck = "org.scala-tools.testing" % "scalacheck_2.9.0.RC1" % "1.9-SNAPSHOT" % "test" // New BSD
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Subprojects
  // -------------------------------------------------------------------------------------------------------------------

  lazy val akka_amqp        = project("akka-amqp", "akka-amqp", new AkkaAMQPProject(_))
  lazy val akka_camel       = project("akka-camel", "akka-camel", new AkkaCamelProject(_))
  lazy val akka_camel_typed = project("akka-camel-typed", "akka-camel-typed", new AkkaCamelTypedProject(_), akka_camel)
  lazy val akka_spring      = project("akka-spring", "akka-spring", new AkkaSpringProject(_), akka_camel, akka_camel_typed)
  lazy val akka_kernel      = project("akka-kernel", "akka-kernel", new AkkaKernelProject(_), akka_spring, akka_amqp, akka_scalaz, akka_disp_extras)
  lazy val akka_scalaz      = project("akka-scalaz", "akka-scalaz", new AkkaScalazProject(_))
  lazy val akka_disp_extras = project("akka-dispatcher-extras", "akka-dispatcher-extras", new AkkaDispatcherExtrasProject(_))
  lazy val akka_sbt_plugin  = project("akka-sbt-plugin",  "akka-sbt-plugin",  new AkkaSbtPluginProject(_))
  lazy val akka_samples     = project("akka-modules-samples", "akka-modules-samples", new AkkaModulesSamplesParentProject(_))

  // -------------------------------------------------------------------------------------------------------------------
  // Miscellaneous
  // -------------------------------------------------------------------------------------------------------------------

  override def disableCrossPaths = true

  // -------------------------------------------------------------------------------------------------------------------
  // Scaladocs
  // -------------------------------------------------------------------------------------------------------------------

  override def docProjectDependencies = dependencies.toList - akka_samples - akka_sbt_plugin

  // -------------------------------------------------------------------------------------------------------------------
  // Publishing
  // -------------------------------------------------------------------------------------------------------------------

  override def managedStyle = ManagedStyle.Maven

  lazy val akkaPublishRepository = systemOptional[String]("akka.publish.repository", "default")
  lazy val akkaPublishCredentials = systemOptional[String]("akka.publish.credentials", "none")

  if (akkaPublishCredentials.value != "none") Credentials(akkaPublishCredentials.value, log)

  def publishToRepository = {
    val repoUrl = akkaPublishRepository.value
    if (repoUrl != "default") Resolver.url("Akka Publish Repository", new java.net.URL(repoUrl))
    else Resolver.file("Local Maven Repository", Path.userHome / ".m2" / "repository" asFile)
  }

  val publishTo = publishToRepository

  override def pomExtra = {
    <inceptionYear>2009</inceptionYear>
    <url>http://akka.io</url>
    <organization>
      <name>Scalable Solutions AB</name>
      <url>http://scalablesolutions.se</url>
    </organization>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
  }

  override def deliverProjectDependencies =
    super.deliverProjectDependencies.toList - akka_sbt_plugin.projectID - akkaDist.projectID

  // -------------------------------------------------------------------------------------------------------------------
  // Build release
  // -------------------------------------------------------------------------------------------------------------------

  val localReleasePath = outputPath / "release" / version.toString
  val localReleaseRepository = Resolver.file("Local Release", localReleasePath / "repository" asFile)
  val localReleaseDownloads = localReleasePath / "downloads"

  override def otherRepositories = super.otherRepositories ++ Seq(localReleaseRepository)

  lazy val publishRelease = {
    val releaseConfiguration = new DefaultPublishConfiguration(localReleaseRepository, "release", false)
    publishTask(publishIvyModule, releaseConfiguration) dependsOn (deliver, publishLocal, makePom)
  }

  lazy val buildRelease = task {
    FileUtilities.copy(Seq(distArchive), localReleaseDownloads, log).left.toOption
  } dependsOn (publishRelease, dist)

  lazy val dist = task { None }
  lazy val distArchive = akkaDist.akkaMicrokernelDist.distArchive

  // -------------------------------------------------------------------------------------------------------------------
  // akka-amqp subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaAMQPProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_actor = Dependencies.akka_actor

    val commons_io = Dependencies.commons_io
    val rabbit     = Dependencies.rabbit
    val protobuf   = Dependencies.protobuf

    // testing
    val junit           = Dependencies.junit
    val multiverse      = Dependencies.multiverse_test
    val scalatest       = Dependencies.scalatest

    override def testOptions = createTestFilter( _.endsWith("Test") )
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-camel subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaCamelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_actor = Dependencies.akka_actor
    val akka_slf4j = Dependencies.akka_slf4j
    val camel_core = Dependencies.camel_core

    // testing
    val junit     = Dependencies.junit
    val scalatest = Dependencies.scalatest
    val logback   = Dependencies.testLogback

    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-camel-typed subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaCamelTypedProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_typed_actor = Dependencies.akka_typed_actor
    val akka_slf4j = Dependencies.akka_slf4j
    val camel_core       = Dependencies.camel_core

    // testing
    val junit     = Dependencies.junit
    val scalatest = Dependencies.scalatest
    val logback   = Dependencies.testLogback

    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-kernel subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaKernelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_stm         = Dependencies.akka_stm
    val akka_remote      = Dependencies.akka_remote
    val akka_http        = Dependencies.akka_http
    val akka_slf4j       = Dependencies.akka_slf4j
    val jetty            = Dependencies.jetty
    val jetty_util       = Dependencies.jetty_util
    val jetty_xml        = Dependencies.jetty_xml
    val jetty_servlet    = Dependencies.jetty_servlet
    val jackson_core     = Dependencies.jackson_core
    val jersey           = Dependencies.jersey
    val jersey_contrib   = Dependencies.jersey_contrib
    val jersey_json      = Dependencies.jersey_json
    val jersey_server    = Dependencies.jersey_server
    val stax_api         = Dependencies.stax_api
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-spring subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaSpringProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_remote      = Dependencies.akka_remote
    val akka_typed_actor = Dependencies.akka_typed_actor
    val spring_beans     = Dependencies.spring_beans
    val spring_context   = Dependencies.spring_context

    // testing
    val camel_spring = Dependencies.camel_spring
    val junit        = Dependencies.junit
    val scalatest    = Dependencies.scalatest
  }

  // -------------------------------------------------------------------------------------------------------------------
  // OSGi stuff
  // -------------------------------------------------------------------------------------------------------------------
/*
  class AkkaOSGiParentProject(info: ProjectInfo) extends ParentProject(info) {
    override def disableCrossPaths = true

    lazy val akka_osgi_dependencies_bundle = project("akka-osgi-dependencies-bundle", "akka-osgi-dependencies-bundle",
      new AkkaOSGiDependenciesBundleProject(_), akka_kernel)

    lazy val akka_osgi_assembly = project("akka-osgi-assembly", "akka-osgi-assembly",
      new AkkaOSGiAssemblyProject(_),
        akka_osgi_dependencies_bundle,
        akka_amqp,
        akka_camel,
        akka_spring)
  }

  class AkkaOSGiDependenciesBundleProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) with BNDPlugin {
    override def bndClasspath = compileClasspath
    override def bndPrivatePackage = Seq("")
    override def bndImportPackage = Seq("*;resolution:=optional")
    override def bndExportPackage = Seq(
      "org.aopalliance.*;version=1.0.0",

      // Provided by other bundles
      "!akka.*",
      "!com.google.inject.*",
      "!javax.ws.rs.*",
      "!javax.jms.*",
      "!javax.transaction,*",
      "!org.apache.commons.io.*",
      "!org.apache.commons.pool.*",
      "!org.codehaus.jackson.*",
      "!org.jboss.netty.*",
      "!org.springframework.*",
      "!org.apache.camel.*",
      "!org.fusesource.commons.management.*",

      "*;version=0.0.0")
  }

  class AkkaOSGiAssemblyProject(info: ProjectInfo) extends DefaultProject(info) with McPom {
    override def disableCrossPaths = true

    // Akka bundles
    val akka_stm         = Dependencies.akka_stm
    val akka_typed_actor = Dependencies.akka_typed_actor
    val akka_remote      = Dependencies.akka_remote
    val akka_http        = Dependencies.akka_http

    // Scala bundle
    val scala_bundle = "com.weiglewilczek.scala-lang-osgi" % "scala-library" % buildScalaVersion % "compile" intransitive

    // Camel bundles
    val camel_core           = Dependencies.camel_core.intransitive
    val fusesource_commonman = "org.fusesource.commonman" % "commons-management" % "1.0" intransitive

    // Spring bundles
    val spring_beans      = Dependencies.spring_beans.intransitive
    val spring_context    = Dependencies.spring_context.intransitive
    val spring_aop        = "org.springframework" % "spring-aop"        % SPRING_VERSION % "compile" intransitive
    val spring_asm        = "org.springframework" % "spring-asm"        % SPRING_VERSION % "compile" intransitive
    val spring_core       = "org.springframework" % "spring-core"       % SPRING_VERSION % "compile" intransitive
    val spring_expression = "org.springframework" % "spring-expression" % SPRING_VERSION % "compile" intransitive
    val spring_jms        = "org.springframework" % "spring-jms"        % SPRING_VERSION % "compile" intransitive
    val spring_tx         = "org.springframework" % "spring-tx"         % SPRING_VERSION % "compile" intransitive

    val commons_codec      = Dependencies.commons_codec.intransitive
    val commons_io         = Dependencies.commons_io.intransitive
    val commons_pool       = Dependencies.commons_pool.intransitive
    val guicey             = Dependencies.guicey.intransitive
    val jackson            = Dependencies.jackson.intransitive
    val jackson_core       = Dependencies.jackson_core.intransitive
    val jsr311             = Dependencies.jsr311.intransitive
    val jta_1_1            = Dependencies.jta_1_1.intransitive
    val netty              = Dependencies.netty.intransitive
    val commons_fileupload = "commons-fileupload"        % "commons-fileupload" % "1.2.1" % "compile" intransitive
    val jms_1_1            = "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "1.1.1" % "compile" intransitive
    val joda               = "joda-time"                 % "joda-time" % "1.6" intransitive

    override def pomPostProcess(node: scala.xml.Node): scala.xml.Node = mcPom(AkkaModulesParentProject.this.moduleConfigurations)(super.pomPostProcess(node))


    override def packageAction =
      task {
        val libs: Seq[Path] = managedClasspath(config("compile")).get.toSeq
        val prjs: Seq[Path] = info.dependencies.toSeq.asInstanceOf[Seq[DefaultProject]] map { _.jarPath }
        val all = libs ++ prjs
        val destination = outputPath / "bundles"
        FileUtilities.copyFlat(all, destination, log)
        log info "Copied %s bundles to %s".format(all.size, destination)
        None
      }

    override def artifacts = Set.empty
  }
*/

  // -------------------------------------------------------------------------------------------------------------------
  // akka-scalaz subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaScalazProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_actor   = Dependencies.akka_actor
    val scalaz       = Dependencies.scalaz

    // testing
    val junit             = Dependencies.junit
    val scalatest         = Dependencies.scalatest
    val scalaz_scalacheck = Dependencies.scalaz_scalacheck
    val scalacheck        = Dependencies.scalacheck
  }

  class AkkaDispatcherExtrasProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val akka_actor    = Dependencies.akka_actor
    val hawt_dispatch = Dependencies.hawtdispatch

    // testing
    val junit             = Dependencies.junit
    val scalatest         = Dependencies.scalatest
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-sbt-plugin subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaSbtPluginProject(info: ProjectInfo) extends PluginProject(info) {
    val srcManagedScala = "src_managed" / "main" / "scala"

    lazy val addAkkaConfig = systemOptional[Boolean]("akka.release", false)

    lazy val generateAkkaSbtPlugin = {
      val cleanSrcManaged = cleanTask(srcManagedScala) named ("clean src_managed")
      task {
        info.parent match {
          case Some(project: ParentProject) =>
            xsbt.FileUtilities.write((srcManagedScala / "AkkaProject.scala").asFile,
                                     GenerateAkkaSbtPlugin(project, AKKA_VERSION, addAkkaConfig.value))
          case _ =>
        }
        None
      } dependsOn cleanSrcManaged
    }

    override def mainSourceRoots = super.mainSourceRoots +++ (srcManagedScala ##)
    override def compileAction = super.compileAction dependsOn(generateAkkaSbtPlugin)

    lazy val publishRelease = {
      val releaseConfiguration = new DefaultPublishConfiguration(localReleaseRepository, "release", false)
      publishTask(publishIvyModule, releaseConfiguration) dependsOn (deliver, publishLocal, makePom)
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Test
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaTypedActorTestProject(info: ProjectInfo) extends DefaultProject(info) {
    // testing
    val junit = "junit" % "junit" % "4.5" % "test"
    val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Samples
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaSampleCamelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info) {
    val activemq      = Dependencies.activemq
    val camel_jetty   = Dependencies.camel_jetty
    val camel_jms     = Dependencies.camel_jms
    val spring_jms    = Dependencies.spring_jms
    val commons_codec = Dependencies.commons_codec

    override def ivyXML = {
      <dependencies>
        <exclude module="slf4j-api" />
      </dependencies>
    }

    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  class AkkaSampleSecurityProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info)

  class AkkaSampleHelloProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info)

  class AkkaModulesSamplesParentProject(info: ProjectInfo) extends ParentProject(info) {
    override def disableCrossPaths = true

    lazy val akka_sample_camel = project("akka-sample-camel", "akka-sample-camel",
      new AkkaSampleCamelProject(_), akka_kernel)

    lazy val akka_sample_security = project("akka-sample-security", "akka-sample-security",
      new AkkaSampleSecurityProject(_), akka_kernel)

    lazy val akka_sample_hello = project("akka-sample-hello", "akka-sample-hello",
      new AkkaSampleHelloProject(_), akka_kernel)

    lazy val publishRelease = {
      val releaseConfiguration = new DefaultPublishConfiguration(localReleaseRepository, "release", false)
      publishTask(publishIvyModule, releaseConfiguration) dependsOn (deliver, publishLocal, makePom)
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Test options
  // -------------------------------------------------------------------------------------------------------------------

  lazy val integrationTestsEnabled = systemOptional[Boolean]("integration.tests",false)
  lazy val stressTestsEnabled = systemOptional[Boolean]("stress.tests",false)

  // -------------------------------------------------------------------------------------------------------------------
  // Default project
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaModulesDefaultProject(info: ProjectInfo) extends DefaultProject(info) with McPom {
    override def disableCrossPaths = true

    override def compileOptions = super.compileOptions ++ scalaCompileSettings.map(CompileOption)
    override def javaCompileOptions = super.javaCompileOptions ++ javaCompileSettings.map(JavaCompileOption)

    override def runClasspath = super.runClasspath +++ (AkkaModulesParentProject.this.info.projectPath / "config")
    override def testClasspath = super.testClasspath +++ (AkkaModulesParentProject.this.info.projectPath / "config")

    lazy val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
    lazy val docsArtifact = Artifact(this.artifactID, "doc", "jar", Some("docs"), Nil, None)

    override def packageDocsJar = this.defaultJarPath("-docs.jar")
    override def packageSrcJar  = this.defaultJarPath("-sources.jar")
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(this.packageDocs, this.packageSrc)

    override def pomPostProcess(node: scala.xml.Node): scala.xml.Node =
      mcPom(AkkaModulesParentProject.this.moduleConfigurations)(super.pomPostProcess(node))

    /**
     * Used for testOptions, possibility to enable the running of integration and or stresstests
     *
     * To enable set true and disable set false
     * set integration.tests true
     * set stress.tests true
     */
    def createTestFilter(defaultTests: (String) => Boolean) = { TestFilter({
        case s: String if defaultTests(s) => true
        case s: String if integrationTestsEnabled.value => s.endsWith("TestIntegration")
        case s: String if stressTestsEnabled.value      => s.endsWith("TestStress")
        case _ => false
      }) :: Nil
    }

    lazy val publishRelease = {
      val releaseConfiguration = new DefaultPublishConfiguration(localReleaseRepository, "release", false)
      publishTask(publishIvyModule, releaseConfiguration) dependsOn (deliver, publishLocal, makePom)
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Distribution
  // -------------------------------------------------------------------------------------------------------------------

  lazy val distExclusive = systemOptional[Boolean]("dist.exclusive", false)

  lazy val akkaDist = project("dist", "akka-dist", new AkkaDistParentProject(_))

  class AkkaDistParentProject(info: ProjectInfo) extends ParentProject(info) {
    lazy val akkaActorsDist = project("actors", "akka-dist-actors", new AkkaActorsDistProject(_))

    lazy val akkaCoreDist = project("core", "akka-dist-core", new AkkaCoreDistProject(_))

    lazy val akkaMicrokernelDist = project("microkernel", "akka-dist-microkernel", new AkkaMicrokernelDistProject(_),
                                           akka_kernel, akka_samples)

    def doNothing = task { None }
    override def publishLocalAction = doNothing
    override def deliverLocalAction = doNothing
    override def publishAction = doNothing
    override def deliverAction = doNothing

    class AkkaActorsDistProject(info: ProjectInfo) extends AkkaDistProject("akka-actors", info) {
      val akkaActor = Dependencies.akka_actor
    }

    class AkkaCoreDistProject(info: ProjectInfo) extends AkkaDistProject("akka-core", info) {
      val akkaRemote = Dependencies.akka_remote
      val akkaSlf4j = Dependencies.akka_slf4j
      val akkaTestkit = Dependencies.akka_testkit
      val akkaActorTests = Dependencies.akka_actor_tests

      override def dependencyClasspath =
        if (distExclusive.value) runClasspath.filter(p => !akkaActorsDist.runClasspath.get.exists(_.name == p.name))
        else runClasspath
    }

    class AkkaMicrokernelDistProject(info: ProjectInfo) extends AkkaDistProject("akka-microkernel", info) {
      override def distConfigSources = (akkaModulesParent.info.projectPath / "config").descendentsExcept("*.*", "*-test.*")
      override def distScriptSources = akkaModulesParent.info.projectPath / "scripts" / "microkernel" * "*"

      override def dependencyClasspath =
        if (distExclusive.value) akka_kernel.runClasspath.filter(p => !akkaCoreDist.runClasspath.get.exists(_.name == p.name))
        else akka_kernel.runClasspath

      override def projectDependencies = akka_kernel.topologicalSort

      override def distAction = super.distAction dependsOn (distSamples)

      lazy val distSamples = distSamplesAction dependsOn (distClean)

      def distSamplesAction = task {
        val demo = akka_samples.akka_sample_hello.jarPath
        val samples = Set(akka_samples.akka_sample_camel,
                          akka_samples.akka_sample_hello,
                          akka_samples.akka_sample_security)

        def distCopySamples[P <: DefaultProject](samples: Set[P]) = {
          samples.map { sample =>
            val sampleOutputPath = distSamplesPath / sample.name
            val binPath = sampleOutputPath / "bin"
            val configPath = sampleOutputPath / "config"
            val deployPath = sampleOutputPath / "deploy"
            val libPath = sampleOutputPath / "lib"
            val srcPath = sampleOutputPath / "src"
            val confs = sample.info.projectPath / "config" ** "*.*"
            val scripts = akkaModulesParent.info.projectPath / "scripts" / "samples" * "*"
            val libs = sample.managedClasspath(Configurations.Runtime)
            val deployed = sample.jarPath
            val sources = sample.packageSourcePaths
            copyFiles(confs, configPath) orElse
            copyScripts(scripts, binPath) orElse
            copyFiles(libs, libPath) orElse
            copyFiles(deployed, deployPath) orElse
            copyPaths(sources, srcPath)
          }.foldLeft(None: Option[String])(_ orElse _)
        }

        copyFiles(demo, distDeployPath) orElse
        distCopySamples(samples)
      }
    }

    class AkkaDistProject(distName: String, info: ProjectInfo) extends DefaultProject(info) {
      val distFullName = distName + "-" + version
      val distOutputBasePath = outputPath / "dist"
      val distOutputPath = (distOutputBasePath ##) / distFullName
      val distScalaLibPath = distOutputPath / "lib"
      val distAkkaPath = distOutputPath
      val distBinPath = distAkkaPath / "bin"
      val distConfigPath = distAkkaPath / "config"
      val distDeployPath = distAkkaPath / "deploy"
      val distLibPath = distAkkaPath / "lib"
      val distSamplesPath = distAkkaPath / "samples"
      val distArchiveName = distFullName + ".zip"
      val distArchive = (distOutputBasePath ##) / distArchiveName

      def distConfigSources = info.projectPath / "config" * "*"
      def distScriptSources = info.projectPath / "scripts" * "*"

      def scalaDependency = if (distExclusive.value) Path.emptyPathFinder else buildLibraryJar

      def dependencyClasspath = runClasspath

      def runtimeJars = (dependencyClasspath
                         .filter(ClasspathUtilities.isArchive)
                         .filter(jar => !jar.name.contains("-sources"))
                         .filter(jar => !jar.name.contains("-docs")))

      def projectDependencies = topologicalSort.dropRight(1)

      def dependencyJars = Path.lazyPathFinder {
        projectDependencies.flatMap( p => p match {
          case pp: PackagePaths => Some(pp.jarPath)
          case _ => None
        })
      }

      def distLibs = runtimeJars +++ dependencyJars

      lazy val dist = distAction dependsOn (`package`, distClean) describedAs("Create a distribution.")

      def distAction = task {
        copyFiles(scalaDependency, distScalaLibPath) orElse
        copyFiles(distLibs, distLibPath) orElse
        copyFiles(distConfigSources, distConfigPath) orElse
        copyScripts(distScriptSources, distBinPath) orElse
        FileUtilities.zip(List(distOutputPath), distArchive, true, log)
      }

      lazy val distClean = distCleanAction describedAs "Clean the dist target dir."

      def distCleanAction = task { FileUtilities.clean(distOutputPath, log) }

      def copyFiles(from: PathFinder, to: Path): Option[String] = {
        if (from.get.isEmpty) None
        else FileUtilities.copyFlat(from.get, to, log).left.toOption
      }

      def copyPaths(from: PathFinder, to: Path): Option[String] = {
        if (from.get.isEmpty) None
        else FileUtilities.copy(from.get, to, log).left.toOption
      }

      def copyScripts(from: PathFinder, to: Path): Option[String] = {
        from.get.map { script =>
          val target = to / script.name
          FileUtilities.copyFile(script, target, log) orElse
          setExecutable(target, script.asFile.canExecute)
        }.foldLeft(None: Option[String])(_ orElse _)
      }

      def setExecutable(target: Path, executable: Boolean): Option[String] = {
        val success = target.asFile.setExecutable(executable, false)
        if (success) None else Some("Couldn't set permissions of " + target)
      }

      override def disableCrossPaths = true

      def doNothing = task { None }
      override def compileAction = doNothing
      override def testCompileAction = doNothing
      override def testAction = doNothing
      override def packageAction = doNothing
      override def publishLocalAction = doNothing
      override def deliverLocalAction = doNothing
      override def publishAction = doNothing
      override def deliverAction = doNothing
    }
  }
}

trait McPom { self: DefaultProject =>
  import scala.xml._

  def mcPom(mcs: Set[ModuleConfiguration])(node: Node): Node = {

    def cleanUrl(url: String) = url match {
      case null => ""
      case "" => ""
      case u if u endsWith "/" => u
      case u => u + "/"
    }

    val oldRepos = (node \\ "project" \ "repositories" \ "repository").
                     map( n => cleanUrl((n \ "url").text) -> (n \ "name").text).toList
    val newRepos = mcs.filter(_.resolver.isInstanceOf[MavenRepository]).map(m => {
                      val r = m.resolver.asInstanceOf[MavenRepository]
                      cleanUrl(r.root) -> r.name
                   })

    val repos = Map((oldRepos ++ newRepos):_*).map( pair =>
                  <repository>
                     <id>{pair._2.toSeq.filter(_.isLetterOrDigit).mkString}</id>
                     <name>{pair._2}</name>
                     <url>{pair._1}</url>
                  </repository>
                )

    def rewrite(pf:PartialFunction[Node,Node])(ns: Seq[Node]): Seq[Node] = for(subnode <- ns) yield subnode match {
        case e: Elem =>
          if (pf isDefinedAt e) pf(e)
          else Elem(e.prefix, e.label, e.attributes, e.scope, rewrite(pf)(e.child):_*)
        case other => other
    }

    val rule: PartialFunction[Node,Node] = if ((node \\ "project" \ "repositories" ).isEmpty) {
      case Elem(prefix, "project", attribs, scope, children @ _*) =>
           Elem(prefix, "project", attribs, scope, children ++ <repositories>{repos}</repositories>:_*)
    } else {
      case Elem(prefix, "repositories", attribs, scope, children @ _*) =>
           Elem(prefix, "repositories", attribs, scope, repos.toList:_*)
    }

    rewrite(rule)(node.theSeq)(0)
  }
}

object GenerateAkkaSbtPlugin {
  def apply(project: ParentProject, akkaVersion: String, addAkkaConfig: Boolean): String = {
    val extraConfigs = {
      if (addAkkaConfig) Set(ModuleConfiguration("se.scalablesolutions.akka", Repositories.AkkaRepo))
      else Set.empty[ModuleConfiguration]
    }
    val akkaModules = project.subProjects.values.map(_.name).flatMap{
      case "akka-sbt-plugin" => Iterator.empty
      case s if s.startsWith("akka-") => Iterator.single(s.drop(5))
      case _ => Iterator.empty
    }
    val (repos, configs) = (project.moduleConfigurations ++ extraConfigs).foldLeft((Set.empty[String], Set.empty[String])){
      case ((repos, configs), ModuleConfiguration(org, name, ver, MavenRepository(repoName, repoPath))) =>
        val repoId = repoName.replaceAll("""[^a-zA-Z]""", "_")
        val configId = org.replaceAll("""[^a-zA-Z]""", "_") +
                         (if (name == "*") "" else ("_" + name.replaceAll("""[^a-zA-Z0-9]""", "_") +
                           (if (ver == "*") "" else ("_" + ver.replaceAll("""[^a-zA-Z0-9]""", "_")))))
        (repos + ("  lazy val "+repoId+" = MavenRepository(\""+repoName+"\", \""+repoPath+"\")"),
        configs + ("  lazy val "+configId+" = ModuleConfiguration(\""+org+"\", \""+name+"\", \""+ver+"\", "+repoId+")"))
      case (x, _) => x
    }
    """|import sbt._
       |
       |object AkkaRepositories {
       |%s
       |}
       |
       |trait AkkaBaseProject extends BasicScalaProject {
       |  import AkkaRepositories._
       |
       |%s
       |}
       |
       |trait AkkaProject extends AkkaBaseProject {
       |  val akkaVersion = "%s"
       |  val akkaModulesVersion = "%s"
       |
       |  def akkaModule(module: String) = "se.scalablesolutions.akka" %% ("akka-" + module) %% {
       |    if (Set(%s).contains(module))
       |      akkaModulesVersion
       |    else
       |      akkaVersion
       |  }
       |
       |  val akkaActor = akkaModule("actor")
       |}
       |""".stripMargin.format(repos.mkString("\n"),
                               configs.mkString("\n"),
                               akkaVersion,
                               project.version.toString,
                               akkaModules.map("\"" + _ + "\"").mkString(", "))
  }
}
