name := "jefe"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.2.4"
scalaVersion in ThisBuild := "2.12.5"
crossScalaVersions in ThisBuild := List("2.12.5")
resolvers in ThisBuild ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
cancelable in Global := true

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "com.outr"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/jefe/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "jefe", "matt@outr.com"))
homepage in ThisBuild := Some(url("https://github.com/outr/jefe"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/outr/jefe"),
    "scm:git@github.com:outr/jefe.git"
  )
)
developers in ThisBuild := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val asmVersion = "6.1.1"
val coursierVersion = "1.0.3"
val libraryManagementVersion = "1.1.4"
val circeVersion = "0.9.3"
val packrVersion = "2.1"
val powerScalaVersion = "2.0.5"
val proguardVersion = "6.0.2"
val scalaXMLVersion = "1.1.0"
val scribeVersion = "2.3.2"

val reactifyVersion = "2.3.0"
val youiVersion = "0.9.0-M8"

lazy val root = project.in(file("."))
  .aggregate(launch, manager, runner, optimizer, pack, server, example)
  .settings(
    publishArtifact := false
  )

lazy val launch = project.in(file("launch"))
  .settings(
    name := "jefe-launch",
    libraryDependencies ++= Seq(
      "com.outr" %% "reactify" % reactifyVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion
    )
  )

lazy val manager = project.in(file("manager"))
  .settings(
    name := "jefe-manager",
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier" % coursierVersion,
      "io.get-coursier" %% "coursier-cache" % coursierVersion,
      "org.scala-sbt" %% "librarymanagement-core" % libraryManagementVersion,
      "org.scala-sbt" %% "librarymanagement-ivy" % libraryManagementVersion,
      "org.powerscala" %% "powerscala-core" % powerScalaVersion,
      "org.powerscala" %% "powerscala-io" % powerScalaVersion,
      "org.scala-lang.modules" %% "scala-xml" % scalaXMLVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion
    )
  )

lazy val runner = project.in(file("runner"))
  .settings(
    name := "jefe-runner",
    assemblyJarName in assembly := "runner.jar"
  )
  .dependsOn(launch, manager)

lazy val optimizer = project.in(file("optimizer"))
  .settings(
    name := "jefe-optimizer",
    libraryDependencies ++= Seq(
      "org.powerscala" %% "powerscala-core" % powerScalaVersion,
      "org.powerscala" %% "powerscala-io" % powerScalaVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion,
      "org.ow2.asm" % "asm" % asmVersion
    )
  )

lazy val pack = project.in(file("pack"))
  .settings(
    name := "jefe-pack",
    libraryDependencies ++= Seq(
      "com.bladecoder.packr" % "packr" % packrVersion,
      "net.sf.proguard" % "proguard-base" % proguardVersion
    )
  )
  .dependsOn(runner, optimizer)

lazy val server = project.in(file("server"))
  .settings(
    name := "jefe-server",
    fork in run := true,
    assemblyJarName := s"${name.value}-${version.value}.jar",
    libraryDependencies ++= Seq(
      "io.youi" %% "youi-server-undertow" % youiVersion,
      "io.youi" %% "youi-client" % youiVersion,
      "org.powerscala" %% "powerscala-concurrent" % powerScalaVersion
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )
  .dependsOn(runner)

lazy val example = project.in(file("example"))
  .settings(
    name := "jefe-example"
  )
  .dependsOn(launch, manager)