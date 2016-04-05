import com.typesafe.sbt.packager.jdkpackager.JDKPackagerPlugin
import com.typesafe.sbt.SbtNativePackager.autoImport._
import sbt.Keys._
import sbt._

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
    NativePackagerKeys.maintainer := "OUTR Technologies, LLC",
    packageSummary := "Example summary",
    packageDescription := "Example description",
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
    .aggregate(launch, manager, example)
    .settings(basicSettings("root"))
    .settings(publishArtifact := false)

  lazy val launch = project.in(file("launch"))
    .settings(basicSettings("launch"))
    .settings(libraryDependencies ++= Seq(metarx, scribeSLF4J))

  lazy val manager = project.in(file("manager"))
    .settings(basicSettings("manager"))
    .settings(libraryDependencies ++= Seq(coursier, coursierCache, powerscala, scalaXML, scribeSLF4J))

  lazy val example = project.in(file("example"))
    .settings(basicSettings("app"))
    .dependsOn(launch, manager)
    .enablePlugins(JDKPackagerPlugin)
}

object Details {
  val organization = "com.outr.jefe"
  val name = "jefe"
  val version = "1.0.0"
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
  val coursier = "com.github.alexarchambault" %% "coursier" % "1.0.0-M10"
  val coursierCache = "com.github.alexarchambault" %% "coursier-cache" % "1.0.0-M10"
  val metarx = "pl.metastack" %% "metarx" % "0.1.6"
  val powerscala = "org.powerscala" %% "powerscala-core" % "2.0.0-SNAPSHOT"
  val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
  val scribeSLF4J = "com.outr.scribe" %% "scribe-slf4j" % "1.2.1"
}
