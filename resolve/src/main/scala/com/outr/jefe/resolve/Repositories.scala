package com.outr.jefe.resolve

case class Repositories(repositories: List[Repository]) {
  def withRepository(repository: Repository): Repositories = Repositories(repositories ::: List(repository))

  def info(artifact: Artifact): Option[ArtifactDetails] = {
    val allInfo = repositories.flatMap(_.info(artifact))
    if (allInfo.nonEmpty) {
      var info = allInfo.head
      allInfo.tail.foreach { i =>
        info = info.merge(i)
      }
      Some(info)
    } else {
      None
    }
  }
}

object Repositories {
  val default: Repositories = apply(Ivy2Local, Maven.Repo1, Sonatype.Releases, Sonatype.Snapshots)

  def apply(repositories: Repository*): Repositories = Repositories(repositories.toList)
}