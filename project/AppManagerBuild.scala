import sbt.Keys._
import sbt._

object AppManagerBuild extends Build {
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
    .aggregate(core)
    .settings(basicSettings("root"))
    .settings(publishArtifact := false)

  lazy val core = project.in(file("core"))
    .settings(basicSettings("core"))
    .settings(libraryDependencies ++= Seq(powerscala, scalaXML))

  lazy val app = project.in(file("app"))
    .settings(basicSettings("app"))
    .dependsOn(core)
}

object Details {
  val organization = "com.outr.appmanager"
  val name = "appmanager"
  val version = "1.0.0-SNAPSHOT"
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
  val powerscala = "org.powerscala" %% "powerscala-core" % "2.0.0-SNAPSHOT"
  val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
}
