package com.outr.appmanager.repo

object Sonatype {
  val Releases = MavenRepository("Sonatype Releases", "https://oss.sonatype.org/content/repositories/releases")
  val Snapshots = MavenRepository("Sonatype Snapshots", "https://oss.sonatype.org/content/repositories/snapshots")
}