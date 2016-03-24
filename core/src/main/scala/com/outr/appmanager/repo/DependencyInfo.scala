package com.outr.appmanager.repo

import org.powerscala.Version

case class DependencyInfo(dependency: Dependency,
                          latest: Version,
                          release: Option[Version],
                          versions: List[Version],
                          lastUpdated: String)
