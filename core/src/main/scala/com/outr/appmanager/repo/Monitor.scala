package com.outr.appmanager.repo
import java.io.File

import com.outr.scribe.Logging

trait Monitor extends coursier.Cache.Logger

object Monitor {
  object Console extends Monitor with Logging {
    override def foundLocally(url: String, f: File): Unit = {
      logger.info(s"foundLocally: ${f.getAbsolutePath}")
    }

    override def downloadingArtifact(url: String, file: File): Unit = {
      logger.info(s"downloadingArtifact: $url, ${file.getAbsolutePath}")
    }

    override def downloadLength(url: String, totalLength: Long, alreadyDownloaded: Long): Unit = {
      logger.info(s"downloadLength: $url, Total: $totalLength, Downloaded: $alreadyDownloaded")
    }

    override def downloadProgress(url: String, downloaded: Long): Unit = {
      logger.info(s"downloadProgress: $url, Downloaded: $downloaded")
    }

    override def downloadedArtifact(url: String, success: Boolean): Unit = {
      logger.info(s"downloadedArtifact: $url, Success: $success")
    }

    override def checkingUpdates(url: String, currentTimeOpt: Option[Long]): Unit = {
      logger.info(s"checkingUpdates: $url, Current Time: $currentTimeOpt")
    }

    override def checkingUpdatesResult(url: String, currentTimeOpt: Option[Long], remoteTimeOpt: Option[Long]): Unit = {
      logger.info(s"checkingUpdatesResult: $url, Current Time: $currentTimeOpt, Remote Time: $remoteTimeOpt")
    }
  }
}