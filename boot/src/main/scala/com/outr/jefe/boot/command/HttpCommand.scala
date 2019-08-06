package com.outr.jefe.boot.command

import java.io.File

import io.youi.client.HttpClient
import io.youi.http.HttpMethod
import io.youi.http.content.Content
import io.youi.net.{ContentType, URL}
import io.youi.stream._
import profig.Profig
import scribe.Execution.global
import perfolation._

import scala.concurrent.Await
import scala.concurrent.duration._

object HttpCommand extends Command {
  override def name: String = "http"

  override def description: String = "Issues an HTTP request and shows an HTTP response"

  override def execute(): Unit = {
    val workingDirectory = new File(Profig("workingDirectory").opt[String].getOrElse("."))
    val url = URL(require("url"))
    val method = HttpMethod(require("method"))
    val contentFile = Profig("content").opt[String].map(fileName => new File(workingDirectory, fileName))
    val contentType = Profig("contentType").opt[String] match {
      case Some(s) => ContentType.parse(s)
      case None => contentFile.map(f => ContentType.byFileName(f.getName)).getOrElse(ContentType.`text/plain`)
    }
    val params = Profig("params").opt[String] match {
      case Some(paramsFile) => {
        val file = new File(workingDirectory, paramsFile)
        val lines = IO.stream(file, new StringBuilder).toString.split('\n').map(_.trim).toList
        val Regex = "(.+?)[=](.+)".r
        var p = url.parameters
        lines.foreach {
          case Regex(key, value) => p = p.withParam(key, value)
        }
        p
      }
      case None => url.parameters
    }
    val content = contentFile.map { file =>
      assert(file.isFile, s"Content references non-existent path: ${file.getAbsolutePath}")
      Content.file(file, contentType)
    }
    val timeout = Profig("timeout").as[Int](10).seconds
    val future = HttpClient
      .url(url.copy(parameters = params))
      .method(method)
      .content(content)
      .send()
    val (response, elapsed) = Time.elapsedReturn {
      Await.result(future, timeout)
    }
    logger.info(s"[$method] $url (${response.status}): Response received after ${elapsed.f()} seconds")
    logger.info("Headers:")
    response.headers.map.foreach {
      case (key, values) => values.foreach { value =>
        logger.info(s"  $key: $value")
      }
    }
    logger.info("Content:")
    response.content.foreach { content =>
      logger.info(content.asString)
    }
    sys.exit(0)
  }

  private def require(key: String): String = Profig(key).as[String](throw new RuntimeException(s"--$key must be specified"))

  override def help(): Unit = {
    logger.info("Usage: jefe http --url=http://localhost:8080/example --method=post --content=example.json --contentType=application/json")
    logger.info("")
    logger.info("Arguments:")
    logger.info("  --url=???: The URL to call")
    logger.info("  --method=???: The HTTP method to use")
    logger.info("  --content=???: Path to the HTTP request content (Defaults to None)")
    logger.info("  --contentType=???: Content-Type for HTTP request content (Defaults to derive from filename of content)")
    logger.info("  --params=???: Path to a file that will be encoded into params by line")
    logger.info("  --workingDirectory=???: The base directory to use (Defaults to current directory)")
    logger.info("  --timeout=???: The number of seconds to wait for a response before timing out (Defaults to 10 seconds)")
  }
}
