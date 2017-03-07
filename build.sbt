name := "jefe"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.0.0"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := List("2.12.1", "2.11.8")
sbtVersion in ThisBuild := "0.13.13"
resolvers in ThisBuild ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")
fork in ThisBuild := true

val coursier = "io.get-coursier" %% "coursier" % "1.0.0-M15-4"
val coursierCache = "io.get-coursier" %% "coursier-cache" % "1.0.0-M15-4"
val reactify = "com.outr" %% "reactify" % "1.4.5-SNAPSHOT"
val packr = "com.bladecoder.packr" % "packr" % "2.1"
val powerScalaCore = "org.powerscala" %% "powerscala-core" % "2.0.5"
val powerScalaIO = "org.powerscala" %% "powerscala-io" % "2.0.5"
val powerScalaCommand = "org.powerscala" %% "powerscala-command" % "2.0.5"
val powerScalaConcurrent = "org.powerscala" %% "powerscala-concurrent" % "2.0.5"
val youIServer = "io.youi" %% "youi-server-undertow" % "0.2.2-SNAPSHOT"
val proguard = "net.sf.proguard" % "proguard-base" % "5.3.2"
val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
val scribe = "com.outr" %% "scribe-slf4j" % "1.4.1"
val asm = "org.ow2.asm" % "asm" % "5.2"

lazy val root = project.in(file("."))
  .aggregate(launch, manager, runner, optimizer, pack, server, console, example)
  .settings(
    publishArtifact := false
  )

lazy val launch = project.in(file("launch"))
  .settings(
    libraryDependencies ++= Seq(
      reactify,
      scribe
    )
  )

lazy val manager = project.in(file("manager"))
  .settings(
    libraryDependencies ++= Seq(
      coursier,
      coursierCache,
      powerScalaCore,
      powerScalaIO,
      scalaXML,
      scribe
    )
  )

lazy val runner = project.in(file("runner"))
  .settings(
    assemblyJarName in assembly := "runner.jar"
  )
  .dependsOn(launch, manager)

lazy val optimizer = project.in(file("optimizer"))
  .settings(
    libraryDependencies ++= Seq(
      powerScalaCore,
      powerScalaIO,
      scribe,
      asm
    )
  )

lazy val pack = project.in(file("pack"))
  .settings(
    libraryDependencies ++= Seq(
      packr,
      proguard
    )
  )
  .dependsOn(runner, optimizer)

lazy val server = project.in(file("server"))
  .settings(
    assemblyJarName := s"${name.value}-${version.value}.jar",
    libraryDependencies ++= Seq(
      youIServer,
      powerScalaCommand,
      powerScalaConcurrent
    )
  )
  .dependsOn(runner)

lazy val console = project.in(file("console"))

lazy val example = project.in(file("example"))
  .dependsOn(launch, manager)