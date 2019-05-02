package spec

import com.outr.jefe.application.ArtifactApplication
import com.outr.jefe.resolve._
import com.outr.jefe.client.JefeClient
import com.outr.jefe.server.{JefeServer, SecurityFilter}
import io.youi.ValidationError
import io.youi.client.HttpClient
import io.youi.http.{HttpRequest, HttpResponse, HttpStatus}
import org.scalatest.concurrent.Eventually
import org.scalatest.{AsyncWordSpec, Matchers, WordSpec}
import profig.Profig
import io.youi.net._
import io.youi.server.ServerUtil
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._

class ClientSpec extends AsyncWordSpec with Matchers with Eventually {
  private lazy val baseURL: URL = url"http://localhost:10565"
  private lazy val client = new JefeClient(baseURL, JefeServer.token)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(15, Millis))
  )

  "Client" should {
    "init properly" in {
      Profig.loadDefaults()
      succeed
    }
    "start the server" in {
      Await.result(JefeServer.start(), 10.seconds)
      JefeServer.isRunning should be(true)
    }
    "create an app" in {
      val config = ArtifactApplication(
        id = "youi-example",
        artifacts = List("io.youi" %% "youi-example" % "latest.release"),
        repositories = Repositories.default,
        mainClass = Some("io.youi.example.ServerExampleApplication"),
        environment = Map.empty
      )
      client.application.create(config).map { response =>
        response.success should be(true)
        response.errors should be(Nil)
      }
    }
    "fail to start the app due to bad security token" in {
      val badClient = new JefeClient(baseURL, "password")
      badClient.application.start("youi-example").map { response =>
        response.success should be(false)
        response.errors should be(List(ValidationError(
          message = "Invalid request, bad jefe.token specified (password)",
          code = SecurityFilter.FailureCode,
          status = HttpStatus.Forbidden
        )))
      }
    }
    "start the app" in {
      ServerUtil.isPortAvailable(8080) should be(true)

      client.application.start("youi-example").map { response =>
        response.success should be(true)
        response.errors should be(Nil)
      }
    }
    "verify the application is running" in {
      eventually {
        ServerUtil.isPortAvailable(8080) should be(false)
      }
      HttpClient.url(url"http://localhost:8080/hello.txt").send().map { response =>
        response.status should be(HttpStatus.OK)
        val content = response.content.getOrElse(fail())
        content.length should be(13L)
      }
    }
    "stop the app" in {
      client.application.stop("youi-example").map { response =>
        response.success should be(true)
        response.errors should be(Nil)
      }
    }
    "stop properly" in {
      JefeServer.dispose()
      JefeServer.isRunning should be(false)
    }
  }
}
