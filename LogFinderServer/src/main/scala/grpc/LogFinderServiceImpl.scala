package grpc

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.Future

class LogFinderServiceImpl(system: ActorSystem[_]) extends LogFinderService {
  private implicit val sys: ActorSystem[_] = system
  private val config: Config = ConfigFactory.load()
  private val APIGatewayURL = config.getString("akka.grpc.server.APIGatewayURL")

  override def findLog(request: FindLogRequest): Future[FindLogReply] = {
    GRPCServer.logger.info("findLog invoked...")
    GRPCServer.logger.info("Calling API Gateway endpoint...")

    val APIGatewayResponse: HttpResponse[String] = Http(APIGatewayURL)
      .param("time", request.time)
      .param("dtInSeconds", request.dtInSeconds)
      .timeout(config.getInt("akka.grpc.server.connectionTimeoutMs"), config.getInt("akka.grpc.server.readTimeoutMs"))
      .asString

    Future.successful(FindLogReply(APIGatewayResponse.body))
  }
}
