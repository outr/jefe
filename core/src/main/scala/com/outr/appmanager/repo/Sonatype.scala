package com.outr.appmanager.repo

object Sonatype {
  val Releases = MavenRepository("https://oss.sonatype.org/content/repositories/releases")
  val Snapshots = MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
}
