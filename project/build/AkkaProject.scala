/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

import com.weiglewilczek.bnd4sbt.BNDPlugin
import java.io.File
import java.util.jar.Attributes
import java.util.jar.Attributes.Name._
import sbt._
import sbt.CompileOrder._
import spde._

class AkkaModulesParentProject(info: ProjectInfo) extends DefaultProject(info) {

  // -------------------------------------------------------------------------------------------------------------------
  // Compile settings
  // -------------------------------------------------------------------------------------------------------------------

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xstrict-warnings",
        "-optimise", //Uncomment this for release compile
        "-Xwarninit",
        "-encoding", "utf8")
        .map(CompileOption(_))
  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // -------------------------------------------------------------------------------------------------------------------
  // Deploy/dist settings
  // -------------------------------------------------------------------------------------------------------------------
  val distName = "%s-%s".format(name, version)
  val distArchiveName = distName + ".zip"
  val deployPath = info.projectPath / "deploy"
  val distPath = info.projectPath / "dist"
  val distArchive = (distPath ##) / distArchiveName

  //The distribution task, packages Akka into a zipfile and places it into the projectPath/dist directory
  lazy val dist = task {

    def transferFile(from: Path, to: Path) =
      if ( from.asFile.renameTo(to.asFile) ) None
      else Some("Couldn't transfer %s to %s".format(from,to))

    //Creates a temporary directory where we can assemble the distribution
    val genDistDir = Path.fromFile({
      val d = File.createTempFile("akka","dist")
      d.delete //delete the file
      d.mkdir  //Recreate it as a dir
      d
    }).## //## is needed to make sure that the zipped archive has the correct root folder

    //Temporary directory to hold the dist currently being generated
    val currentDist = genDistDir / distName

    FileUtilities.copy(allArtifacts.get, currentDist, log).left.toOption orElse //Copy all needed artifacts into the root archive
    FileUtilities.zip(List(currentDist), distArchiveName, true, log) orElse //Compress the root archive into a zipfile
    transferFile(info.projectPath / distArchiveName, distArchive) orElse //Move the archive into the dist folder
    FileUtilities.clean(genDistDir,log) //Cleanup the generated jars

  } dependsOn (`package`) describedAs("Zips up the distribution.")

  // -------------------------------------------------------------------------------------------------------------------
  // All repositories *must* go here! See ModuleConigurations below.
  // -------------------------------------------------------------------------------------------------------------------

  object Repositories {
    lazy val AkkaRepo               = MavenRepository("Akka Repository", "http://akka.io/repository")
    lazy val ScalaToolsRepo         = MavenRepository("Scala-Tools Repo", "http://scala-tools.org/repo-releases")
    lazy val ScalaToolsSnapshotRepo = MavenRepository("Scala-Tools Snapshot Repo", "http://scala-tools.org/repo-snapshots")
    lazy val CodehausRepo           = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
    lazy val LocalMavenRepo         = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
    lazy val FusesourceSnapshotRepo = MavenRepository("Fusesource Snapshots", "http://repo.fusesource.com/nexus/content/repositories/snapshots")
    lazy val GuiceyFruitRepo        = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo              = MavenRepository("JBoss Repo", "http://repository.jboss.org/nexus/content/groups/public/")
    lazy val JavaNetRepo            = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
    lazy val SonatypeSnapshotRepo   = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
    lazy val SunJDMKRepo            = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
    lazy val ClojarsRepo            = MavenRepository("Clojars Repo", "http://clojars.org/repo")
    lazy val ScalaToolsRelRepo      = MavenRepository("Scala Tools Releases Repo", "http://scala-tools.org/repo-releases")
  }

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
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest", ScalaToolsRelRepo)
  lazy val sjsonModuleConfig       = ModuleConfiguration("net.debasishg", ScalaToolsRelRepo)
  lazy val atomikosModuleConfig    = ModuleConfiguration("com.atomikos",sbt.DefaultMavenRepository)
  lazy val timeModuleConfig        = ModuleConfiguration("org.scala-tools", "time", ScalaToolsRepo)
  lazy val args4jModuleConfig      = ModuleConfiguration("args4j", JBossRepo)
  lazy val scannotationModuleConfig= ModuleConfiguration("org.scannotation", JBossRepo)
  lazy val scalazModuleConfig      = ModuleConfiguration("org.scalaz", ScalaToolsSnapshotRepo)
  lazy val lzfModuleConfig         = ModuleConfiguration("voldemort.store.compress", "h2-lzf", AkkaRepo)
  lazy val aspectWerkzModuleConfig = ModuleConfiguration("org.codehaus.aspectwerkz", "aspectwerkz", "2.2.3", AkkaRepo)
  lazy val rabbitModuleConfig      = ModuleConfiguration("com.rabbitmq","rabbitmq-client", "0.9.1", AkkaRepo)
  val localMavenRepo               = LocalMavenRepo // Second exception, also fast! ;-)

  // -------------------------------------------------------------------------------------------------------------------
  // Versions
  // -------------------------------------------------------------------------------------------------------------------

  lazy val AKKA_VERSION          = "1.1-SNAPSHOT"
  lazy val ATMO_VERSION          = "0.6.2"
  lazy val CAMEL_VERSION         = "2.5.0"
  lazy val DISPATCH_VERSION      = "0.7.4"
  lazy val HAWT_DISPATCH_VERSION = "1.1"
  lazy val JACKSON_VERSION       = "1.7.1"
  lazy val JERSEY_VERSION        = "1.3"
  lazy val MULTIVERSE_VERSION    = "0.6.2"
  lazy val SCALATEST_VERSION     = "1.3"
  lazy val SLF4J_VERSION         = "1.6.0"
  lazy val SPRING_VERSION        = "3.0.4.RELEASE"
  lazy val JETTY_VERSION         = "7.1.6.v20100715"
  lazy val CODEC_VERSION         = "1.4"

  // -------------------------------------------------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------------------------------------------------

  object Dependencies {

    // Compile
    lazy val akka_actor       = "se.scalablesolutions.akka" % "akka-actor"       % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_stm         = "se.scalablesolutions.akka" % "akka-stm"         % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_remote      = "se.scalablesolutions.akka" % "akka-remote"      % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_http        = "se.scalablesolutions.akka" % "akka-http"        % AKKA_VERSION % "compile" //ApacheV2
    lazy val akka_typed_actor = "se.scalablesolutions.akka" % "akka-typed-actor" % AKKA_VERSION % "compile" //ApacheV2

    lazy val aopalliance = "aopalliance" % "aopalliance" % "1.0" % "compile" //Public domain

    lazy val aspectwerkz = "org.codehaus.aspectwerkz" % "aspectwerkz" % "2.2.3" % "compile" //LGPL 2.1

    lazy val camel_core = "org.apache.camel" % "camel-core" % CAMEL_VERSION % "compile" //ApacheV2

    lazy val commons_codec = "commons-codec" % "commons-codec" % CODEC_VERSION % "compile" //ApacheV2

    lazy val commons_io = "commons-io" % "commons-io" % "1.4" % "compile" //ApacheV2

    lazy val commons_pool = "commons-pool" % "commons-pool" % "1.5.4" % "compile" //ApacheV2

    lazy val guicey = "org.guiceyfruit" % "guice-all" % "2.0" % "compile" //ApacheV2

    lazy val h2_lzf = "voldemort.store.compress" % "h2-lzf" % "1.0" % "compile" //ApacheV2

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

    lazy val rabbit = "com.rabbitmq" % "amqp-client" % "1.8.1" % "compile" //Mozilla public license

    lazy val scalaz = "org.scalaz" % "scalaz-core_2.8.1" % "6.0-SNAPSHOT" % "compile" //New BSD

    lazy val sjson = "net.debasishg" % "sjson_2.8.1" % "0.9.1" % "compile" //ApacheV2
    lazy val sjson_test = "net.debasishg" % "sjson_2.8.1" % "0.9.1" % "test" //ApacheV2

    lazy val spring_beans   = "org.springframework" % "spring-beans"   % SPRING_VERSION % "compile" //ApacheV2
    lazy val spring_context = "org.springframework" % "spring-context" % SPRING_VERSION % "compile" //ApacheV2

    lazy val google_coll    = "com.google.collections" % "google-collections"  % "1.0"             % "compile" //ApacheV2

    // Test

    lazy val camel_spring   = "org.apache.camel"       % "camel-spring"        % CAMEL_VERSION     % "test" //ApacheV2
    lazy val commons_coll   = "commons-collections"    % "commons-collections" % "3.2.1"           % "test" //ApacheV2

    lazy val testJetty      = "org.eclipse.jetty"      % "jetty-server"        % JETTY_VERSION     % "test" //Eclipse license
    lazy val testJettyWebApp= "org.eclipse.jetty"      % "jetty-webapp"        % JETTY_VERSION     % "test" //Eclipse license

    lazy val junit          = "junit"                  % "junit"               % "4.5"             % "test" //Common Public License 1.0
    lazy val mockito        = "org.mockito"            % "mockito-all"         % "1.8.1"           % "test" //MIT
    lazy val scalatest      = "org.scalatest"          % "scalatest"           % SCALATEST_VERSION % "test" //ApacheV2
    lazy val specs          = "org.scala-tools.testing" %% "specs"             % "1.6.6"           % "test" //MIT

    lazy val scalaz_scalacheck = "org.scalaz" % "scalaz-scalacheck-binding_2.8.1" % "6.0-SNAPSHOT" % "test" //New BSD
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Subprojects
  // -------------------------------------------------------------------------------------------------------------------

  lazy val akka_amqp        = project("akka-amqp", "akka-amqp", new AkkaAMQPProject(_))
  lazy val akka_camel       = project("akka-camel", "akka-camel", new AkkaCamelProject(_))
  lazy val akka_camel_typed = project("akka-camel-typed", "akka-camel-typed", new AkkaCamelTypedProject(_), akka_camel)
  lazy val akka_spring      = project("akka-spring", "akka-spring", new AkkaSpringProject(_), akka_camel, akka_camel_typed)
  lazy val akka_kernel      = project("akka-kernel", "akka-kernel", new AkkaKernelProject(_), akka_spring, akka_amqp)
  lazy val akka_osgi        = project("akka-osgi", "akka-osgi", new AkkaOSGiParentProject(_))
  lazy val akka_scalaz      = project("akka-scalaz", "akka-scalaz", new AkkaScalazProject(_))
  lazy val akka_samples     = project("akka-samples", "akka-samples", new AkkaSamplesParentProject(_))

  // -------------------------------------------------------------------------------------------------------------------
  // Miscellaneous
  // -------------------------------------------------------------------------------------------------------------------
  override def disableCrossPaths = true

  override def mainClass = Some("akka.kernel.Main")

  override def packageOptions =
    manifestClassPath.map(cp => ManifestAttributes(
      (Attributes.Name.CLASS_PATH, cp),
      (IMPLEMENTATION_TITLE, "Akka Modules"),
      (IMPLEMENTATION_URL, "http://akka.io"),
      (IMPLEMENTATION_VENDOR, "Scalable Solutions AB")
    )).toList :::
    getMainClass(false).map(MainClass(_)).toList

  // create a manifest with all akka jars and dependency jars on classpath
  override def manifestClassPath = Some(allArtifacts.getFiles
    .filter(_.getName.endsWith(".jar"))
    .filter(!_.getName.contains("servlet_2.4"))
    .filter(!_.getName.contains("scala-library"))
    .map("lib_managed/compile/" + _.getName)
    .mkString(" ") +
    " config/" +
    " scala-library.jar" +
    " dist/akka-camel-%s.jar".format(version) +
    " dist/akka-amqp-%s.jar".format(version) +
    " dist/akka-kernel-%s.jar".format(version) +
    " dist/akka-spring-%s.jar".format(version) +
    " dist/akka-scalaz-%s.jar".format(version)
    )

  //Exclude slf4j1.5.11 from the classpath, it's conflicting...
  override def fullClasspath(config: Configuration): PathFinder = {
    super.fullClasspath(config) ---
    (super.fullClasspath(config) ** "slf4j*1.5.11.jar")
  }

  override def mainResources = super.mainResources +++
          (info.projectPath / "config").descendentsExcept("*", "logback-test.xml")

  override def runClasspath = super.runClasspath +++ "config"

  // ------------------------------------------------------------
  // publishing
  override def managedStyle = ManagedStyle.Maven
  //override def defaultPublishRepository = Some(Resolver.file("maven-local", Path.userHome / ".m2" / "repository" asFile))
  val publishTo = Resolver.file("maven-local", Path.userHome / ".m2" / "repository" asFile)

  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "doc", "jar", Some("docs"), Nil, None)

  // Credentials(Path.userHome / ".akka_publish_credentials", log)

  //override def documentOptions = encodingUtf8.map(SimpleDocOption(_))
  override def packageDocsJar = defaultJarPath("-docs.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)

  override def pomExtra =
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

  // publish to local mvn
  import Process._
  lazy val publishLocalMvn = runMvnInstall
  def runMvnInstall = task {
    for (absPath <- akkaArtifacts.getPaths) {
      val artifactRE = """(.*)/dist/(.*)-(\d.*)\.jar""".r
      val artifactRE(path, artifactId, artifactVersion) = absPath
      val command = "mvn install:install-file" +
                    " -Dfile=" + absPath +
                    " -DgroupId=se.scalablesolutions.akka" +
                    " -DartifactId=" + artifactId +
                    " -Dversion=" + version +
                    " -Dpackaging=jar -DgeneratePom=true"
      command ! log
    }
    None
  } dependsOn(dist) describedAs("Run mvn install for artifacts in dist.")


  // Build release

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


  // -------------------------------------------------------------------------------------------------------------------
  // akka-amqp subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaAMQPProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
    val akka_remote = Dependencies.akka_remote

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

  class AkkaCamelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
    val akka_actor = Dependencies.akka_actor
    val camel_core = Dependencies.camel_core

    // testing
    val junit     = Dependencies.junit
    val scalatest = Dependencies.scalatest
    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-camel-typed subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaCamelTypedProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
    val akka_typed_actor = Dependencies.akka_typed_actor
    val camel_core       = Dependencies.camel_core

    // testing
    val junit     = Dependencies.junit
    val scalatest = Dependencies.scalatest
    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-kernel subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaKernelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
    val akka_stm         = Dependencies.akka_stm
    val akka_remote      = Dependencies.akka_remote
    val akka_http        = Dependencies.akka_http
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

  class AkkaSpringProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
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

  // -------------------------------------------------------------------------------------------------------------------
  // akka-scalaz subproject
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaScalazProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) {
    val akka_actor   = Dependencies.akka_actor
    val scalaz       = Dependencies.scalaz
    val hawtdispatch = Dependencies.hawtdispatch

    // testing
    val junit             = Dependencies.junit
    val scalatest         = Dependencies.scalatest
    val scalaz_scalacheck = Dependencies.scalaz_scalacheck
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
  // Examples
  // -------------------------------------------------------------------------------------------------------------------

  class AkkaSampleAntsProject(info: ProjectInfo) extends DefaultSpdeProject(info) {
    override def disableCrossPaths = true
    override def spdeSourcePath = mainSourcePath / "spde"
  }

  class AkkaSampleCamelProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, deployPath) {
    //Must be like this to be able to exclude the geronimo-servlet_2.4_spec which is a too old Servlet spec
    override def ivyXML =
      <dependencies>
        <dependency org="org.springframework" name="spring-jms" rev={SPRING_VERSION}>
        </dependency>
        <dependency org="org.apache.geronimo.specs" name="geronimo-servlet_2.5_spec" rev="1.1.1">
        </dependency>
        <dependency org="org.apache.camel" name="camel-jetty" rev={CAMEL_VERSION}>
          <exclude module="geronimo-servlet_2.4_spec"/>
        </dependency>
        <dependency org="org.apache.camel" name="camel-jms" rev={CAMEL_VERSION}>
        </dependency>
        <dependency org="org.apache.activemq" name="activemq-core" rev="5.3.2">
        </dependency>
      </dependencies>

    override def testOptions = createTestFilter( _.endsWith("Test"))
  }

  class AkkaSampleSecurityProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, deployPath) {
    val commons_codec = Dependencies.commons_codec
    val jsr250        = Dependencies.jsr250
    val jsr311        = Dependencies.jsr311
  }

  class AkkaSampleOSGiProject(info: ProjectInfo) extends AkkaModulesDefaultProject(info, distPath) with BNDPlugin {
    val akka_remote = Dependencies.akka_remote
    val osgi_core   = Dependencies.osgi_core

    override lazy val bndBundleActivator = Some("akka.sample.osgi.Activator")
    override lazy val bndExportPackage = Nil // Necessary because of mixing-in AkkaModulesDefaultProject which exports all ...akka.* packages!
  }

  class AkkaSamplesParentProject(info: ProjectInfo) extends ParentProject(info) {
    override def disableCrossPaths = true

    lazy val akka_sample_camel = project("akka-sample-camel", "akka-sample-camel",
      new AkkaSampleCamelProject(_), akka_kernel)
    lazy val akka_sample_security = project("akka-sample-security", "akka-sample-security",
      new AkkaSampleSecurityProject(_), akka_kernel)
    lazy val akka_sample_osgi = project("akka-sample-osgi", "akka-sample-osgi",
      new AkkaSampleOSGiProject(_))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------------------------------------

  def removeDupEntries(paths: PathFinder) =
   Path.lazyPathFinder {
     val mapped = paths.get map { p => (p.relativePath, p) }
     (Map() ++ mapped).values.toList
   }

  def allArtifacts = {
    Path.fromFile(buildScalaInstance.libraryJar) +++
    (removeDupEntries(runClasspath filter ClasspathUtilities.isArchive) +++
    ((outputPath ##) / defaultJarName) +++
    mainResources +++
    mainDependencies.scalaJars +++
    descendents(info.projectPath / "scripts", "run_akka.sh") +++
    descendents(info.projectPath / "scripts", "akka-init-script.sh") +++
    descendents(info.projectPath / "dist", "*.jar") +++
    descendents(info.projectPath / "deploy", "*.jar") +++
    descendents(path("lib") ##, "*.jar") +++
    descendents(configurationPath(Configurations.Compile) ##, "*.jar"))
    .filter(jar => // remove redundant libs
      !jar.toString.endsWith("stax-api-1.0.1.jar") ||
      !jar.toString.endsWith("scala-library-2.7.7.jar")
    )
  }

  def akkaArtifacts = descendents(info.projectPath / "dist", "*-" + version + ".jar")
  lazy val integrationTestsEnabled = systemOptional[Boolean]("integration.tests",false)
  lazy val stressTestsEnabled = systemOptional[Boolean]("stress.tests",false)

  // ------------------------------------------------------------
  class AkkaModulesDefaultProject(info: ProjectInfo, val deployPath: Path) extends DefaultProject(info) with DeployProject with OSGiProject with McPom {
    override def disableCrossPaths = true
    lazy val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
    lazy val docsArtifact = Artifact(this.artifactID, "doc", "jar", Some("docs"), Nil, None)
    override def runClasspath = super.runClasspath +++ (AkkaModulesParentProject.this.info.projectPath / "config")
    override def testClasspath = super.testClasspath +++ (AkkaModulesParentProject.this.info.projectPath / "config")
    override def packageDocsJar = this.defaultJarPath("-docs.jar")
    override def packageSrcJar  = this.defaultJarPath("-sources.jar")
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(this.packageDocs, this.packageSrc)
    override def pomPostProcess(node: scala.xml.Node): scala.xml.Node = mcPom(AkkaModulesParentProject.this.moduleConfigurations)(super.pomPostProcess(node))


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
}

trait DeployProject { self: BasicScalaProject =>
  // defines where the deployTask copies jars to
  def deployPath: Path

  lazy val dist = deployTask(jarPath, packageDocsJar, packageSrcJar, deployPath, true, true, true) dependsOn(
    `package`, packageDocs, packageSrc) describedAs("Deploying")
  def deployTask(jar: Path, docs: Path, src: Path, toDir: Path,
                 genJar: Boolean, genDocs: Boolean, genSource: Boolean) = task {
    def gen(jar: Path, toDir: Path, flag: Boolean, msg: String): Option[String] =
    if (flag) {
      log.info(msg + " " + jar)
      FileUtilities.copyFile(jar, toDir / jar.name, log)
    } else None

    gen(jar, toDir, genJar, "Deploying bits") orElse
    gen(docs, toDir, genDocs, "Deploying docs") orElse
    gen(src, toDir, genSource, "Deploying sources")
  }
}

trait OSGiProject extends BNDPlugin { self: DefaultProject =>
  override def bndExportPackage = Seq("akka.*;version=%s".format(projectVersion.value))
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
