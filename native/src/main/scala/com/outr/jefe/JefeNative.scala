package com.outr.jefe

import java.io.File
import java.nio.file.{Files, LinkOption, Path, Paths}

import com.softwaremill.sttp.quick._

object JefeNative {
  private val CheckVersionTimeout: Long = 24 * 60 * 60 * 1000
  private val MavenMetadataURL = uri"http://repo1.maven.org/maven2/com/outr/jefe-runner_2.12/maven-metadata.xml"

  // TODO: support other paths
  private lazy val curl = new File("/usr/bin/curl")
  private lazy val wget = new File("/usr/bin/wget")
  private lazy val nohup = new File("/usr/bin/nohup")

  def main(args: Array[String]): Unit = {
    // Determine the home directory for Jefe
    val userHome = Paths.get(System.getProperty("user.home"))
    val directory = userHome.resolve(".jefe")
    Files.createDirectories(directory)

    // Determine the latest version of Jefe from Maven Central
    val latestVersionFile = directory.resolve("latest.version")
    val latestVersion = if (!Files.isRegularFile(latestVersionFile) || System.currentTimeMillis() - Files.getLastModifiedTime(latestVersionFile).toMillis > CheckVersionTimeout) {
      // Download latest version information
      val ReleaseRegex = """.*[<]release[>](.+)[<][/]release[>]""".r
      val version = linesFromMaven().collectFirst {
        case ReleaseRegex(v) => v
      }.getOrElse(throw new RuntimeException(s"Unable to find release in $MavenMetadataURL"))
      Files.write(latestVersionFile, version.getBytes("UTF-8"))
      version
    } else {
      // Use cached version
      new String(Files.readAllBytes(latestVersionFile), "UTF-8")
    }

    // Download the latest assembly JAR if not already available
    val download = directory.resolve("download")
    Files.createDirectories(download)
    val assemblyJAR = download.resolve(s"jefe-boot-assembly-$latestVersion.jar")
    val assemblyJARTemp = download.resolve(s"jefe-boot-assembly-$latestVersion.jar.tmp")
    if (Files.notExists(assemblyJAR)) {
      Files.deleteIfExists(assemblyJARTemp)
      val jar = s"http://repo1.maven.org/maven2/com/outr/jefe-runner_2.12/$latestVersion/jefe-runner_2.12-$latestVersion.jar"
      println(s"Downloading $jar...")
      saveURL(jar, assemblyJARTemp)
      Files.move(assemblyJARTemp, assemblyJAR)
    }

    // Determine the location of JRE
    val javaHome = Paths.get(determineJavaHome())
    val javaBin = javaHome.resolve("bin")
    val java = javaBin.resolve("java")

    // TODO: Run the jar


    println(s"Hello, World! $directory, Latest Version: [$latestVersion], Java? $java")
  }

  private def linesFromMaven(): List[String] = {
    val request = sttp.get(MavenMetadataURL)
    val response = request.send()
    response.unsafeBody.split('\n').toList.map(_.trim)
  }

  private def determineJavaHome(): String = Option(System.getenv("JAVA_HOME")) match {
    case Some(home) => home
    case None => throw new RuntimeException(s"No JAVA_HOME environment variable set")
  }

  private def saveURL(url: String, output: Path): Unit = {
    val outputPath = output.toFile.getAbsolutePath
    val pb = if (wget.exists()) {
      new ProcessBuilder(wget.getAbsolutePath, "-O", outputPath, url)
    } else if (curl.exists()) {
      new ProcessBuilder(curl.getAbsolutePath, "--output", outputPath, url)
    } else {
      throw new RuntimeException("Unable to find wget or curl!")
    }
    val process = pb.start()
    val exitValue = process.waitFor()
    if (exitValue != 0) {
      throw new RuntimeException(s"Process $pb returned $exitValue")
    }
  }
}