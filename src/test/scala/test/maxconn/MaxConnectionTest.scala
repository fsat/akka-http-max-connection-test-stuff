package test.maxconn

import akka.actor.{ReceiveTimeout, Terminated, ActorRef, Actor}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.pattern._
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent}
import org.scalatest.{Matchers, WordSpec}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MaxConnectionTest {
}

class MaxConnectionTest extends WordSpec with Matchers {
  "max connection" should {
    "allow new connection when no of connection < max" in pending
    "disallow new connection when no of connection >= max" in pending
  }
}
