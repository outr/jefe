package com.outr.jefe.resolve

case class ArtifactDetails(artifact: Artifact,
                           latest: Option[Version],
                           release: Option[Version],
                           versions: List[Version]) {
  def max(v1: Option[Version], v2: Option[Version]): Option[Version] = (v1, v2) match {
    case (Some(version1), Some(version2)) => if (version1 > version2) {
      Some(version1)
    } else {
      Some(version2)
    }
    case (Some(version1), None) => Some(version1)
    case (None, Some(version2)) => Some(version2)
    case (None, None) => None
  }

  def merge(that: ArtifactDetails): ArtifactDetails = {
    assert(this.artifact == that.artifact, s"Can only merge for the same artifact. ${this.artifact} != ${that.artifact}")
    val latest = max(this.latest, that.latest)
    val release = max(this.release, that.release)
    val versions = (this.versions ::: that.versions).distinct.sorted.reverse
    ArtifactDetails(artifact, latest, release, versions)
  }
}
