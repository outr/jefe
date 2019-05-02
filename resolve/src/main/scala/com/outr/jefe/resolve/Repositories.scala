package com.outr.jefe.resolve

import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import profig.JsonUtil

case class Repositories(repositories: List[Repository]) {
  def withRepository(repository: Repository): Repositories = Repositories(repositories ::: List(repository))
  def withRepositories(repositories: Repository*): Repositories = Repositories(this.repositories ::: repositories.toList)

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

  implicit val encoder: Encoder[Repositories] = new Encoder[Repositories] {
    override def apply(repositories: Repositories): Json = {
      val entries = repositories.repositories.map {
        case Ivy2Local => Json.fromString("ivy2Local")
        case repo: MavenRepository => JsonUtil.toJson[MavenRepository](repo)
      }
      Json.arr(entries: _*)
    }
  }

  implicit val decoder: Decoder[Repositories] = new Decoder[Repositories] {
    override def apply(c: HCursor): Result[Repositories] = {
      val json = c.value
      val repositories = (json \\ "repositories").headOption match {
        case Some(repos) => {   // Legacy support
          repos.asArray.get.toList.map { entry =>
            if ((entry \\ "Ivy2Local").nonEmpty) {
              Ivy2Local
            } else {
              val maven = (entry \\ "MavenRepository").head
              val name = (maven \\ "name").head.asString.get
              val url = (maven \\ "url").head.asString.get
              val credentials = (maven \\ "credentials").headOption.flatMap(creds => JsonUtil.fromJson[Option[Credentials]](creds))
              MavenRepository(name, url, credentials)
            }
          }
        }
        case None => {          // New functionality
          json.asArray.get.toList.map { entry =>
            entry.asString match {
              case Some("ivy2Local") => Ivy2Local
              case Some(s) => throw new RuntimeException(s"Invalid repository entry: $s")
              case None => JsonUtil.fromJson[MavenRepository](entry)
            }
          }
        }
      }
      Right(Repositories(repositories))
    }
  }

  def apply(repositories: Repository*): Repositories = Repositories(repositories.toList)
}