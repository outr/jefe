package com.outr.jefe.resolve

object Test {
  def main(args: Array[String]): Unit = {
    val repositories = Repositories.default
    val artifact = "io.youi" %% "youi-example" % "latest.release"
    val manager = ArtifactManager(repositories, CoursierResolver)
    val files = manager.resolve(artifact)
    scribe.info(s"Files: ${files.map(_.getName).mkString(", ")}")
  }
}
