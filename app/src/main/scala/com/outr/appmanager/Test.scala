package com.outr.appmanager

import java.io.File
import java.net.URLClassLoader

import com.outr.scribe.formatter.FormatterBuilder
import com.outr.scribe.{LogHandler, Logger, Logging}
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.osgi.updatesite.UpdateSiteResolver
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.plugins.resolver.{ChainResolver, FileSystemResolver, IBiblioResolver, IvyRepResolver, URLResolver}
import org.apache.ivy.util.filter.FilterHelper
import coursier._

import scalaz.concurrent.Task

object Test extends App with Logging {
  Logger.Root.clearHandlers()
  Logger.Root.addHandler(LogHandler(formatter = FormatterBuilder().date().string(" - ").message.newLine))

  val directory = new File("cache")

//  testCoursier("org.scalarelational", "scalarelational-core_2.11", "1.3.0")
//  testCoursier("com.cedarsoft.commons", "image", "7.1.0")
  testCoursier("com.outr.hw", "hello-world_2.11", "1.0.0-SNAPSHOT")

//  testIvy("log4j", "log4j", "1.2.16")
//  testIvy("org.scalatest", "scalatest_2.11", "latest.release")
//  testIvy("org.scalarelational", "scalarelational-core_2.11", "1.3.0")
//  testIvy("com.outr.hw", "hello-world_2.11", "1.0.0-SNAPSHOT")

  def testCoursier(groupId: String, artifactId: String, version: String): Unit = {
    val start = Resolution(Set(Dependency(Module(groupId, artifactId), version)))
    val repositories = Seq(Cache.ivy2Local, Cache.ivy2Cache, MavenRepository("https://repo1.maven.org/maven2"))
    val fetch = Fetch.from(repositories, Cache.fetch())
    val resolution = start.process.run(fetch).run
    val errors = resolution.errors
    val conflicts = resolution.conflicts

    if (errors.nonEmpty) {
      throw new RuntimeException(s"Errors: $errors")
    }
    if (conflicts.nonEmpty) {
      throw new RuntimeException(s"Conflicts: $conflicts")
    }

    val localArtifacts = Task.gatherUnordered(
      resolution.artifacts.map(Cache.file(_).run)
    ).run
    val fileErrors = localArtifacts.map(_.toEither).collect {
      case Left(err) => err
    }
    if (fileErrors.nonEmpty) {
      throw new RuntimeException(s"File Errors: $fileErrors")
    }
    val files = localArtifacts.map(_.toEither).collect {
      case Right(f) => f
    }
    println(s"Files: ${files.map(_.getAbsolutePath).mkString(", ")}")
    val classLoader = new URLClassLoader(files.map(_.toURI.toURL).toArray, null)
    val c = classLoader.loadClass("com.outr.hw.HelloWorld")
    println(s"Class: $c")
    val main = c.getMethod("main", classOf[Array[String]])
    println(s"Main: $main")
    main.invoke(null, Array[String]())
  }

  def testIvy(groupId: String, artifactId: String, version: String): Unit = {
    val settings = new IvySettings
//    settings.setBaseDir(directory)
//    settings.setDefaultCache(directory)

    val chain = new ChainResolver
    chain.setName("chain")
    chain.setSettings(settings)

    val central = new IBiblioResolver {
      setM2compatible(true)
      setUsepoms(true)
      setName("central")
      setSettings(settings)
    }
//    settings.addResolver(central)
    chain.add(central)

    val ivy2Directory = new File(System.getProperty("user.home"), ".ivy2")
    val local = new FileSystemResolver
    local.setCheckmodified(true)
    local.setName("local")
    local.addIvyPattern(s"${ivy2Directory.toURI.toURL.toExternalForm}local/[module](-[branch])/[revision]/[artifact](-[branch])-[revision].[ext]")
    local.addArtifactPattern(s"${ivy2Directory.toURI.toURL.toExternalForm}local/[module](-[branch])/[revision]/[artifact](-[branch])-[revision].[ext]")

//    val local = new URLResolver
//    local.setName("local")
//    val directory = new File(System.getProperty("user.home"), ".ivy2/local")
//    println(s"Directory: ${directory.getAbsolutePath} / ${directory.toURI.toURL.toExternalForm} / ${directory.exists()}")
//    local.addArtifactPattern(s"${directory.toURI.toURL.toExternalForm}")
//    local.setSettings(settings)
//    settings.addResolver(local)
    chain.add(local)

    settings.addResolver(chain)
    settings.setDefaultResolver(chain.getName)

//    val resolver = new URLResolver
//    resolver.setM2compatible(true)
//    resolver.setName("central")
//    resolver.addArtifactPattern("http://repo1.maven.org/maven2/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
//    settings.addResolver(resolver)

//    val sonatypeReleases = new URLResolver
//    sonatypeReleases.setM2compatible(true)
//    sonatypeReleases.setName("Sonatype Releases")
//    sonatypeReleases.addArtifactPattern("https://oss.sonatype.org/content/repositories/releases/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
//    settings.addResolver(sonatypeReleases)

    val ivy = Ivy.newInstance(settings)
    val ivyFile = File.createTempFile("ivy", ".xml")
    ivyFile.deleteOnExit()

    val md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(groupId, s"$artifactId-caller", "working"))
    val dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(groupId, artifactId, version), false, false, true)
    md.addDependency(dd)

    XmlModuleDescriptorWriter.write(md, ivyFile)

    val resolveOptions = new ResolveOptions().setConfs(Array("default"))
    resolveOptions.setTransitive(true)
    resolveOptions.setDownload(true)
    resolveOptions.setArtifactFilter(FilterHelper.getArtifactTypeFilter("jar"))
    val report = ivy.resolve(ivyFile, resolveOptions)

    val artifactReports = report.getAllArtifactsReports
    val jar = artifactReports.head.getLocalFile

    println(s"JAR: ${jar.getAbsolutePath}")
    println(s"Artifacts: ${artifactReports.map(adr => adr.getLocalFile).map(_.getName).mkString(", ")}")
  }

  def testRepo(): Unit = {
    /*// TODO: support parents in Maven
    // TODO: support version lookup when no version is provided

    //  val dependency = "org.scalarelational" %% "scalarelational-core"
    val dependency = "org.scalatest" %% "scalatest"
    val repos = List(Ivy2.Local, Ivy2.Cache, Sonatype.Snapshots, Sonatype.Releases, Maven.Repo1)
    val info = Repository.info(dependency, repos: _*).get
    val version = info.release.get

    //  version.dependencies.foreach(d => println(s"Dep: $d / ${d.scope}"))

    //  val lookup = "org.scoverage" % "scalac-scoverage-runtime_2.11" % "1.0.1"
    //  val updated = lookup.resolve(repos: _*)
    //  println(s"Found in repo: ${updated.repository}")

    val resolver = new DependencyResolver(version, repos)
    resolver.resolve()
    logger.info(s"Dependencies: ${resolver.dependencies.keys.mkString(", ")}")
    logger.info(s"Latest: ${resolver.latest.mkString(", ")}")

    val versionDirectory = new File(directory, version.version.toString())
    versionDirectory.mkdirs()
    resolver.download(versionDirectory, latestOnly = true, useCache = true)

    //  println(Ivy2.Local.info(scalaRelational))
    //  println(Ivy2.Cache.info(scalaTest))
    //  val info = Sonatype.Releases.info(scalaRelational).get
    //  println(s"Info: $info, ${info.latest.major} / ${info.latest.minor} / ${info.latest.extra}")*/
  }
}