package com.outr.jefe.resolve

object Sonatype {
  lazy val Releases: MavenRepository = MavenRepository("Sonatype Releases", "https://oss.sonatype.org/content/repositories/releases")
  lazy val Snapshots: MavenRepository = MavenRepository("Sonatype Snapshots", "https://oss.sonatype.org/content/repositories/snapshots")
}
