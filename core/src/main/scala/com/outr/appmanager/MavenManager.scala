package com.outr.appmanager

case class MavenManager(baseURL: String = "https://oss.sonatype.org/content/repositories/releases") {
}

case class Dependency(group: String, name: String)

case class DependencyInstance(dependency: Dependency, manager: MavenManager) {
  def latestVersion: Option[String] = {
    val url = s"${manager.baseURL}/${dependency.group.replace('.', '/')}/${dependency.name}"
    val metadataURL = s"$url/maven-metadata.xml"
    println(s"Metadata URL: $metadataURL")
    None
  }
}