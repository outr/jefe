package com.outr.jefe.repo

import java.awt.{BorderLayout, Font, GraphicsEnvironment}
import java.io.File
import javax.swing.{BorderFactory, JDialog, JFrame, JLabel, JPanel, JProgressBar, SwingConstants, SwingUtilities}

import com.outr.scribe.Logging

trait Monitor extends Logging {
  private var downloading = Map.empty[String, Downloading]
  private var _downloaded = 0L
  private var _length = 0L

  def downloaded: Long = _downloaded
  def length: Long = _length
  def percent: Double = if (downloaded == 0L || length == 0L) {
    0.0
  } else {
    (downloaded.toDouble / length.toDouble) * 100.0
  }

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
    var frequency = 100L

    private var lastLogged = 0L

    override def modified(): Unit = {
      val now = System.currentTimeMillis()
      if (now > lastLogged + frequency) {
        logger.info(s"Downloading $downloaded of $length (${math.floor(percent)}%)")
        lastLogged = now
      }
    }
  }

  object Dialog extends JDialog with Monitor {
    lazy val dialog = new MonitorDialog(this)

    override def modified(): Unit = {
      dialog.update()
    }
  }
}

class MonitorDialog(monitor: Monitor) extends JDialog(null.asInstanceOf[JFrame], "Downloading") {
  val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
  val center = ge.getCenterPoint

  setSize(600, 300)
  setLocation(math.round(center.getX - (getWidth / 2.0)).toInt, math.round(center.getY - (getHeight / 2.0)).toInt)

  val panel = new JPanel(new BorderLayout(10, 10))
  setContentPane(panel)

  val label = new JLabel("Updating Application", SwingConstants.CENTER) {
    setFont(getFont.deriveFont(Font.BOLD, 34.0f))
  }
  panel.add(label, BorderLayout.CENTER)

  val progress = new JProgressBar {
    setMinimum(0)
    setMaximum(100)
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
    setStringPainted(true)
  }
  panel.add(progress, BorderLayout.SOUTH)

  var lastUpdated = 0L
  var timeout = 1000L
  val hider = new Thread {
    override def run(): Unit = {
      while (true) {
        Thread.sleep(10)

        if (isVisible) {
          val now = System.currentTimeMillis()
          if (now > lastUpdated + timeout) {
            SwingUtilities.invokeAndWait(new Runnable {
              override def run(): Unit = setVisible(false)
            })
          }
        }
      }
    }

    setDaemon(true)
  }
  hider.start()

  def update(): Unit = {
    lastUpdated = System.currentTimeMillis()

    SwingUtilities.invokeAndWait(new Runnable {
      override def run(): Unit = {
        progress.setValue(math.floor(monitor.percent).toInt)
        progress.setString(s"${monitor.downloaded} of ${monitor.length}")

        setVisible(true)
      }
    })
  }
}