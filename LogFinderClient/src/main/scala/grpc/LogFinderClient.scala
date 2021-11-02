package grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Client that performs calls to the GRPC Server to find logs in the specified time intervals
 */
object LogFinderClient {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "LogFinderClient")
    implicit val ec: ExecutionContext = sys.executionContext
    val logger: Logger = LoggerFactory.getLogger(classOf[grpc.LogFinderClient.type])
    val config: Config = ConfigFactory.load()

    val client = LogFinderServiceClient(GrpcClientSettings.fromConfig("logfinder.LogFinderService").withTls(false))

    singleRequestReply(config.getString("akka.grpc.client.example_time"), config.getString("akka.grpc.client.example_dt_seconds"))

    def singleRequestReply(time: String, dtInSeconds: String): Unit = {
      println(s"Performing request, input: time = $time, dt = $dtInSeconds")
      val reply = client.findLog(FindLogRequest(time, dtInSeconds))
      reply.onComplete {
        case Success(msg) =>
          logger.info(s"Received reply: $msg")
        case Failure(e) =>
          logger.error(s"Error: $e")
      }
    }
  }
}
