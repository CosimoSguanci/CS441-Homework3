package grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// Skeleton implementation reference: https://developer.lightbend.com/guides/akka-grpc-quickstart-scala/

/**
 * Objects that implements a GRPC Server, that handles the calls from the Client to find logs.
 */
object GRPCServer {
  val logger: Logger = LoggerFactory.getLogger(classOf[grpc.GRPCServer.type])

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "LogFinderServer", conf)
    new GRPCServer(system).run()
  }
}

class GRPCServer(system: ActorSystem[_]) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    val config: Config = ConfigFactory.load()

    val service: HttpRequest => Future[HttpResponse] =
      LogFinderServiceHandler(new LogFinderServiceImpl(system))

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "0.0.0.0", port = config.getInt("akka.grpc.server.port"))
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        GRPCServer.logger.info("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        GRPCServer.logger.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }
}
