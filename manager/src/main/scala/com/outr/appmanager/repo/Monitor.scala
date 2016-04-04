package com.outr.appmanager.repo
import java.io.File

import com.outr.scribe.Logging

trait Monitor extends Logging {
  private var downloading = Map.empty[String, Downloading]
  private var _downloaded = 0L
  private var _length = 0L

  def downloaded: Long = _downloaded
  def length: Long = _length

  def modified(): Unit

  private[repo] val cacheLogger = new coursier.Cache.Logger {
    override def downloadingArtifact(url: String, file: File): Unit = synchronized {
      downloading += url -> Downloading(url, file, 0L, 0L)
      modified()
    }

    override def downloadLength(url: String, totalLength: Long, alreadyDownloaded: Long): Unit = synchronized {
      downloading.get(url) match {
        case Some(d) => {
          _length += totalLength
          _downloaded += alreadyDownloaded
          d.length = totalLength
          d.downloaded = alreadyDownloaded
        }
        case None => logger.warn(s"Unable to find Downloading for $url in downloadLength.")
      }
      modified()
    }

    override def downloadProgress(url: String, downloaded: Long): Unit = synchronized {
      downloading.get(url) match {
        case Some(d) => {
          _downloaded += (downloaded - d.downloaded)
          d.downloaded = downloaded
        }
        case None => logger.warn(s"Unable to find Downloading for $url in downloadProgress.")
      }
      modified()
    }

    override def downloadedArtifact(url: String, success: Boolean): Unit = synchronized {
      downloading.get(url) match {
        case Some(d) => d.success = success
        case None => logger.warn(s"Unable to find Downloading for $url in downloadedArtifact.")
      }
      modified()
    }
  }
}

case class Downloading(url: String, file: File, var downloaded: Long, var length: Long, var success: Boolean = false)

object Monitor {
  object Console extends Monitor with Logging {
    override def modified(): Unit = logger.info(s"Downloading $downloaded of $length")
  }
}