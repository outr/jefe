package spec

import com.outr.jefe.resolve._
import org.scalatest.{Matchers, WordSpec}

class ResolveSpec extends WordSpec with Matchers {
  "Resolve" when {
    "using coursier" should {
      "resolve a specific version" in {
        val version = "0.9.0-M16"
        val repositories = Repositories.default
        val artifact = "io.youi" %% "youi-example" % version
        val manager = ArtifactManager(repositories, CoursierResolver)
        val files = manager.resolve(artifact)
        files.length should be(54)
        files.find(_.getName == s"youi-example_$CurrentScalaVersion-$version.jar") should not be None
      }
      // TODO: Figure out why this is failing with Coursier
      /*"resolve the latest release" in {
        val repositories = Repositories.default
        val artifact = "io.youi" %% "youi-example" % "latest.release"
        val manager = ArtifactManager(repositories, CoursierResolver)
        val latest = manager.release(artifact.artifact).map(_.version).getOrElse(fail())
        val files = manager.resolve(artifact)
        files.length should be(54)
        files.find(_.getName == s"youi-example_$CurrentScalaVersion-$latest.jar") should not be None
      }*/
    }
    "using SBT" should {
      "resolve a specific version" in {
        val version = "0.9.0-M16"
        val repositories = Repositories.default
        val artifact = "io.youi" %% "youi-example" % version
        val manager = ArtifactManager(repositories, SBTResolver)
        val files = manager.resolve(artifact)
        files.length should be(162)
        files.find(_.getName == s"youi-example_$CurrentScalaVersion-$version.jar") should not be None
      }
      "resolve the latest release" in {
        val repositories = Repositories.default
        val artifact = "io.youi" %% "youi-example" % "latest.release"
        val manager = ArtifactManager(repositories, SBTResolver)
        val latest = manager.release(artifact.artifact).map(_.version).getOrElse(fail())
        val files = manager.resolve(artifact)
        files.length should be(147)
        files.find(_.getName == s"youi-example_$CurrentScalaVersion-$latest.jar") should not be None
      }
    }
  }
}