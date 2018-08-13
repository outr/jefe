package spec

import com.outr.jefe.application.{ApplicationManager, ProcessApplication}
import io.youi.client.HttpClient
import io.youi.http.{HttpRequest, HttpResponse, HttpStatus}
import io.youi.net._
import io.youi.server.ServerUtil
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import profig.Profig
import com.outr.jefe.resolve._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApplicationSpec extends WordSpec with Matchers with Eventually {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(15, Millis))
  )

  "Application" should {
    "init properly" in {
      Profig.loadDefaults()
    }
    "create app" in {
      val app = ProcessApplication.artifact(
        id = "youi-example",
        artifacts = List("io.youi" %% "youi-example" % "latest.release"),
        mainClass = Some("io.youi.example.ServerExampleApplication")
      )
      ApplicationManager += app
    }
    "launch app" in {
      ServerUtil.isPortAvailable(8080) should be(true)
      val app = ApplicationManager.byId("youi-example").getOrElse(fail()).asInstanceOf[ProcessApplication]
      ApplicationManager.launch(app)
      eventually(app.isRunning)
    }
    "verify app launched successfully" in {
      eventually {
        ServerUtil.isPortAvailable(8080) should be(false)
      }
      val client = HttpClient()
      val response: HttpResponse = Await.result(client.send(HttpRequest(url = url"http://localhost:8080/hello.txt")), Duration.Inf)
      response.status should be(HttpStatus.OK)
      val content = response.content.getOrElse(fail())
      content.length should be(13L)
    }
    "stop properly" in {
      ApplicationManager.dispose()
    }
  }
}
