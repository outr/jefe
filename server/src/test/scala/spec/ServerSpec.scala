package spec

import com.outr.jefe.application.ArtifactApplication
import com.outr.jefe.server.JefeServer
import org.scalatest._
import profig.Profig
import com.outr.jefe.resolve._
import io.youi.client.HttpClient
import io.youi.http.{HttpResponse, HttpStatus}
import io.youi.net._
import io.youi.server.ServerUtil
import org.scalatest.concurrent.Eventually
import org.scalatest.time._

import scala.concurrent.Await
import scala.concurrent.duration._
import scribe.Execution.global

class ServerSpec extends WordSpec with Matchers with Eventually {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(15, Millis))
  )

  "Server" should {
    "init properly" in {
      Profig.loadDefaults()
    }
    "start properly" in {
      JefeServer.isRunning should be(false)
      Await.result(JefeServer.start(), 10.seconds)
      JefeServer.isRunning should be(true)
    }
    "create app" in {
      val app = ArtifactApplication(
        id = "youi-example",
        artifacts = List("io.youi" %% "youi-example" % "latest.release"),
        mainClass = Some("io.youi.example.ServerExampleApplication")
      )
      JefeServer.applications += app
    }
    "launch app" in {
      ServerUtil.isPortAvailable(8080) should be(true)
      val app = JefeServer.applications.byId("youi-example").getOrElse(fail()).asInstanceOf[ArtifactApplication]
      JefeServer.applications.launch(app)
      eventually(app.isRunning)
    }
    "verify app launched successfully" in {
      eventually {
        ServerUtil.isPortAvailable(8080) should be(false)
      }
      val client = HttpClient.url(url"http://localhost:8080/hello.txt")
      val response: HttpResponse = Await.result(client.send(), Duration.Inf)
      response.status should be(HttpStatus.OK)
      val content = response.content.getOrElse(fail())
      content.length should be(13L)
    }
    "stop properly" in {
      JefeServer.dispose()
      JefeServer.isRunning should be(false)
    }
  }
}