package test.maxconn

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import akka.pattern._
import de.heikoseeberger.akkasse.{ServerSentEvent, EventStreamUnmarshalling}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.duration._

class MaxConnectionTest extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 20.seconds)

  val actorSystemServer = ActorSystem("max-connection-test-server")
  val actorSystemClient = ActorSystem("max-connection-test-client")

  override protected def afterAll(): Unit = {
    actorSystemServer.shutdown()
    actorSystemClient.shutdown()
  }

  "max connection" should {
    "disallow new connection when no of connection >= max" in {
      import EventStreamUnmarshalling._

      val host = "localhost"
      val port = 7411
      val maxConnection = 1
      Main.startServer(host, port, maxConnection)(actorSystemServer)

      val client = Http(actorSystemClient)

      implicit val actorSystemForTest = actorSystemClient
      implicit val materializer = ActorMaterializer()
      import actorSystemForTest.dispatcher

      val publisherCount = TestProbe()

      publisherCount.awaitAssert {
        client
          .singleRequest(HttpRequest(uri = s"http://$host:$port/publisher-count"))
          .flatMap(v => Unmarshal(v.entity).to[String])
          .map(_.toInt)
          .pipeTo(publisherCount.ref)

        publisherCount.expectMsg(0)
      }

      val sseSourceProbe = TestProbe()

      client
        .singleRequest(HttpRequest(uri = s"http://$host:$port/events"))
        .flatMap(v => Unmarshal(v.entity).to[Source[ServerSentEvent, Any]])
        .pipeTo(sseSourceProbe.ref)

      sseSourceProbe
        .expectMsgType[Source[ServerSentEvent, Any]]
        .grouped(10)
        .runWith(Sink.head)

      publisherCount.awaitAssert {
        client
          .singleRequest(HttpRequest(uri = s"http://$host:$port/publisher-count"))
          .flatMap(v => Unmarshal(v.entity).to[String])
          .map(_.toInt)
          .pipeTo(publisherCount.ref)

        publisherCount.expectMsg(1)
      }

      val sseSourceProbe2 = TestProbe()

      client
        .singleRequest(HttpRequest(uri = s"http://$host:$port/events"))
        .flatMap(v => Unmarshal(v.entity).to[Source[ServerSentEvent, Any]])
        .pipeTo(sseSourceProbe2.ref)

      sseSourceProbe2
        .expectMsgType[Source[ServerSentEvent, Any]]
        .grouped(10)
        .runWith(Sink.head)

      publisherCount.awaitAssert {
        client
          .singleRequest(HttpRequest(uri = s"http://$host:$port/publisher-count"))
          .flatMap(v => Unmarshal(v.entity).to[String])
          .map(_.toInt)
          .pipeTo(publisherCount.ref)

        publisherCount.expectMsg(2)
      }

      fail(s"Should not have gone this far - the number of connections open > max connection ($maxConnection)")
    }

  }
}
