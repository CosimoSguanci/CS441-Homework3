package rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{formFields, get, parameters, post}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpResponse}

/**
 * Defines endpoints behavior
 *
 * @param logFinderActorRef reference to the log finder actor
 * @param system            Actor System
 */
class RouteConfig(implicit val logFinderActorRef: ActorRef,
                  implicit val system: ActorSystem) {

  private val logger: Logger = LoggerFactory.getLogger(classOf[RouteConfig])
  private val config: Config = ConfigFactory.load()
  private val APIGatewayURL = config.getString("restServer.APIGatewayURL")

  val getRoute: Route =
    parameters("time", "dtInSeconds") { (time, dtInSeconds) =>
      get {
        logger.info("Handling GET request...")
        handleFindLogsRequest(time, dtInSeconds)
      }
    }

  val postRoute: Route =
    formFields("time", "dtInSeconds") { (time, dtInSeconds) =>
      post {
        logger.info("Handling POST request...")
        handleFindLogsRequest(time, dtInSeconds)
      }

    }

  /**
   * Handles both POST and GET requests, it forwards calls to the API Gateway Endpoint and return the result to the client
   *
   * @param time        the time passed by the Client
   * @param dtInSeconds the time delta passed by the Client, used to compute the time interval to be considered
   * @return the MD5 of the Regex instances of found logs, or "not found"
   */
  def handleFindLogsRequest(time: String, dtInSeconds: String) = {
    logger.info("Calling API Gateway endpoint...")
    val APIGatewayResponse: HttpResponse[String] = Http(APIGatewayURL)
      .param("time", time).param("dtInSeconds", dtInSeconds)
      .timeout(config.getInt("restServer.connectionTimeoutMs"), config.getInt("restServer.readTimeoutMs"))
      .asString
    RouteDirectives.complete(HttpEntity(APIGatewayResponse.body))
  }
}
