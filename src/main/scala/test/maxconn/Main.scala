package test.maxconn

import akka.actor._
import akka.http.ServerSettings
import akka.http.scaladsl.Http
import akka.pattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkasse.{EventStreamMarshalling, ServerSentEvent}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Main {
  sealed trait ActorMessage
  case object GetSourceRequest extends ActorMessage
  case object GetPublisherCountRequest extends ActorMessage
  case class RegisterNewPublisher(ref: ActorRef) extends ActorMessage
  case class Tick(number: Long) extends ActorMessage

  def fromTickToSse(tick: Tick): ServerSentEvent =
    ServerSentEvent(eventType = Some("tick"), data = s"${tick.number}")

  object GenerateEventsActor {
    def props: Props = Props(new GenerateEventsActor)
  }

  class GenerateEventsActor extends Actor with ActorLogging {
    override def preStart(): Unit = {
      context.setReceiveTimeout(1.second)
    }

    override def receive: Receive = handleAll(Seq.empty)

    private def handleAll(publishers: Seq[ActorRef]): Receive =
      handleGetSource
        .orElse(handleRegisterNewPublisher(publishers))
        .orElse(handleGetPublisherCount(publishers))
        .orElse(handleTerminated(publishers))
        .orElse(handleReceiveTimeout(publishers))

    private def handleGetSource: Receive = {
      case GetSourceRequest =>
        val selfRef = self
        val source = Source.actorRef[Tick](100, OverflowStrategy.dropHead)
          .map(fromTickToSse)
          .mapMaterializedValue(selfRef ! RegisterNewPublisher(_))
        sender() ! source
    }

    private def handleGetPublisherCount(publishers: Seq[ActorRef]): Receive = {
      case GetPublisherCountRequest =>
        sender() ! publishers.size
    }

    private def handleRegisterNewPublisher(publishers: Seq[ActorRef]): Receive = {
      case RegisterNewPublisher(ref) =>
        log.info(s"New publisher $ref")
        context.watch(ref)
        context.become(handleAll(publishers :+ ref))
    }

    private def handleTerminated(publishers: Seq[ActorRef]): Receive = {
      case Terminated(ref) =>
        log.info(s"Publisher terminated $ref")
        context.become(handleAll(publishers.filterNot(_ == ref)))
    }

    private def handleReceiveTimeout(publishers: Seq[ActorRef]): Receive = {
      case ReceiveTimeout =>
        val tick = Tick(System.currentTimeMillis())
        publishers.foreach(_ ! tick)
    }
  }

  def route(generateEvents: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Route = {
    import Directives._
    import EventStreamMarshalling._

    path("events") {
      get {
        onComplete(
          generateEvents.ask(GetSourceRequest)
            .mapTo[Source[ServerSentEvent, Unit]]
        ) {
          case Success(source) =>
            complete(source)

          case Failure(exception) =>
            complete(StatusCodes.InternalServerError -> s"${exception.getClass.getName}: ${exception.getMessage}")
        }
      }
    } ~
    path("publisher-count") {
      get {
        onComplete(
          generateEvents.ask(GetPublisherCountRequest)
            .mapTo[Int]
        ) {
          case Success(value) =>
            complete(s"$value")

          case Failure(exception) =>
            complete(StatusCodes.InternalServerError -> s"${exception.getClass.getName}: ${exception.getMessage}")
        }
      }
    }
  }

  def startServer(host: String, port: Int, maxConnection: Int)(implicit actorSystem: ActorSystem): Unit = {
    import actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()

    val generateEvents = actorSystem.actorOf(GenerateEventsActor.props)
    val routeToMount = route(generateEvents)(5.seconds, actorSystem.dispatcher)

    val customSettings = ServerSettings(actorSystem).copy(maxConnections = maxConnection)
    val log = actorSystem.log

    Http(actorSystem)
      .bindAndHandle(routeToMount, host, port, settings = customSettings)
      .foreach { _ =>
        log.info(s"Listening on $host:$port with maxConnection of $maxConnection")
        log.info(s"To test max-connection, open multiple SSE using: `curl $host:$port/events`")
      }
  }

  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("main")
    startServer("localhost", 7070, maxConnection = 1)
  }
}
