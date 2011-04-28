/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

import sbt._
import java.util.jar.Attributes.Name._

trait AkkaKernelProject extends AkkaProject {
  // automatic akka kernel dependency
  val akkaKernel = akkaModule("kernel")

  def distOutputPath = outputPath / "dist"

  def distLibName = "lib"
  def distStartJarName = "start.jar"
  def distConfigName = "config"
  def distDeployName = "deploy"

  def distLibPath = distOutputPath / distLibName
  def distStartJarPath = distOutputPath / distStartJarName
  def distConfigPath = distOutputPath / distConfigName
  def distDeployPath = distOutputPath / distDeployName

  def runtimeJars = {
    runClasspath
    .filter(ClasspathUtilities.isArchive)
    .filter(jar => !jar.name.contains("-sources"))
    .filter(jar => !jar.name.contains("-docs"))
  }

  def distLibs = runtimeJars +++ buildLibraryJar

  def distConfigSources = "config" ** "*.*"

  def dependencyJars = dependencies.flatMap( _ match {
    case pp: PackagePaths => Some(pp.jarPath)
    case _ => None
  })

  def distDeployJars = Seq(jarPath) ++ dependencyJars

  lazy val dist = (distAction dependsOn (`package`, distStartJar)
                   describedAs "Create an Akka microkernel distribution.")

  def distAction = task {
    log.info("Creating distribution %s ..." format distOutputPath)
    FileUtilities.copyFlat(distLibs.get, distLibPath, log).left.toOption orElse
    FileUtilities.copyFlat(distConfigSources.get, distConfigPath, log).left.toOption orElse
    writeFiles(distOutputPath, distScripts) orElse
    FileUtilities.copyFlat(distDeployJars, distDeployPath, log).left.toOption orElse {
      log.info("Distribution created.")
      None
    }
  }

  lazy val distStartJar = (packageTask(Path.emptyPathFinder, distStartJarPath, distPackageOptions)
                           dependsOn distClean
                           describedAs "Create a dist start jar with manifest classpath.")

  def distExtraClasspath = Seq(distConfigName + "/")

  def distLib(jar: Path) = distLibName + "/" + jar.name

  def distManifestClasspath = (distLibs.get.map(distLib) ++ distExtraClasspath).mkString(" ")

  def distPackageOptions = Seq(
    ManifestAttributes(
      (CLASS_PATH, distManifestClasspath),
      (IMPLEMENTATION_TITLE, "Akka Microkernel"),
      (IMPLEMENTATION_URL, "http://akka.io"),
      (IMPLEMENTATION_VENDOR, "Scalable Solutions AB")),
    MainClass("akka.kernel.Main"))

  lazy val distClean = distCleanAction describedAs "Clean the dist target dir."

  def distCleanAction = task { FileUtilities.clean(distOutputPath, log) }

  def writeFiles(path: Path, files: Seq[(String, String)]) = {
    files.map(s => FileUtilities.write((path / s._1).asFile, s._2, log))
    .foldLeft(None: Option[String])(_ orElse _)
  }

  def distScripts = Seq(("start.sh", distShScript), ("start.bat", distBatScript))

  def distShScript = """|microkernel=`dirname $0`
                        |java -Xmx512M -Dakka.home=$microkernel -jar $microkernel/%s
                        |""".stripMargin.format(distStartJarName)

  def distBatScript = """|set MICROKERNEL=%%~dp0
                         |java -Xmx512M -Dakka.home=%%MICROKERNEL%% -jar "%%MICROKERNEL%%%s"
                         |""".stripMargin.format(distStartJarName)
}
