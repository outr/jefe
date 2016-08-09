import sbt.Keys._
import sbt._

import sbtassembly.AssemblyKeys._

object JefeBuild extends Build {
  import Dependencies._

  private def basicSettings(moduleName: String) = Seq(
    name := s"${Details.name}-$moduleName",
    version := Details.version,
    organization := Details.organization,
    scalaVersion := Details.scalaVersion,
    sbtVersion := Details.sbtVersion,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases")
    ),
    fork := true,
    connectInput := true,
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT")) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
        } else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
        }
    },
    publishArtifact in Test := false,
    pomExtra :=
      <url>${Details.url}</url>
      <licenses>
        <license>
          <name>{Details.licenseType}</name>
          <url>{Details.licenseURL}</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:{Details.repoURL}</developerConnection>
        <connection>scm:{Details.repoURL}</connection>
        <url>{Details.projectURL}</url>
      </scm>
      <developers>
        <developer>
          <id>{Details.developerId}</id>
          <name>{Details.developerName}</name>
          <url>{Details.developerURL}</url>
        </developer>
      </developers>
  )

  lazy val root = project.in(file("."))
    .aggregate(launch, manager, runner, optimizer, pack, server, example)
    .settings(basicSettings("root"))
    .settings(publishArtifact := false)
  lazy val launch = project.in(file("launch"))
    .settings(basicSettings("launch"))
    .settings(libraryDependencies ++= Seq(metarx, scribe))
  lazy val manager = project.in(file("manager"))
    .settings(basicSettings("manager"))
    .settings(libraryDependencies ++= Seq(coursier, coursierCache, powerscalaCore, powerscalaIO, scalaXML, scribe))
  lazy val runner = project.in(file("runner"))
    .settings(basicSettings("runner"))
    .settings(assemblyJarName in assembly := "runner.jar")
    .dependsOn(launch, manager)
  lazy val optimizer = project.in(file("optimizer"))
    .settings(basicSettings("optimizer"))
    .settings(libraryDependencies ++= Seq(powerscalaCore, scribe))
  lazy val pack = project.in(file("pack"))
    .settings(basicSettings("pack"))
    .settings(libraryDependencies ++= Seq(packr, proguard))
    .dependsOn(runner, optimizer)
  lazy val server = project.in(file("server"))
    .settings(basicSettings("server"))
    .settings(assemblyJarName := s"${name.value}-${version.value}.jar")
    .settings(libraryDependencies ++= Seq(undertowCore, powerscalaCommand, powerscalaConcurrent))
    .dependsOn(runner)
  lazy val example = project.in(file("example"))
    .settings(basicSettings("app"))
    .dependsOn(launch, manager)
}

object Details {
  val organization = "com.outr.jefe"
  val name = "jefe"
  val version = "1.0.1"
  val url = "http://outr.com"
  val licenseType = "MIT"
  val licenseURL = "http://opensource.org/licenses/MIT"
  val projectURL = "https://github.com/outr/app-manager"
  val repoURL = "https://github.com/outr/app-manager.git"
  val developerId = "darkfrog"
  val developerName = "Matt Hicks"
  val developerURL = "http://matthicks.com"

  val sbtVersion = "0.13.11"
  val scalaVersion = "2.11.8"
}

object Dependencies {
  val coursier = "io.get-coursier" %% "coursier" % "1.0.0-M12-1"
  val coursierCache = "io.get-coursier" %% "coursier-cache" % "1.0.0-M12-1"
  val metarx = "pl.metastack" %% "metarx" % "0.1.7"
  val packr = "com.badlogicgames.packr" % "packr" % "2.0-SNAPSHOT"
  val powerscalaCore = "org.powerscala" %% "powerscala-core" % "2.0.2-SNAPSHOT"
  val powerscalaIO = "org.powerscala" %% "powerscala-io" % "2.0.2-SNAPSHOT"
  val powerscalaCommand = "org.powerscala" %% "powerscala-command" % "2.0.2-SNAPSHOT"
  val powerscalaConcurrent = "org.powerscala" %% "powerscala-concurrent" % "2.0.2-SNAPSHOT"
  val proguard = "net.sf.proguard" % "proguard-base" % "5.2.1"
  val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
  val scribe = "com.outr.scribe" %% "scribe-slf4j" % "1.2.3"
  val undertowCore = "io.undertow" % "undertow-core" % "1.4.0.CR1"
}
