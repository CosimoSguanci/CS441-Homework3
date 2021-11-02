package rest

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{get, parameters, withRequestTimeout}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{PathDirectives, RouteDirectives}
import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.duration.DurationInt


class RouteConfig(implicit val logFinderActorRef: ActorRef,
                  implicit val system: ActorSystem) {

  private val config: Config = ConfigFactory.load()
  private val APIGatewayURL = config.getString("restServer.APIGatewayURL")

  val getRoute: Route =
    parameters("time", "dtInSeconds") { (time, dtInSeconds) =>
      withRequestTimeout(30.seconds) {
        PathDirectives.pathPrefix("findlogs") {
          get {
            val APIGatewayResponse: HttpResponse[String] = Http(APIGatewayURL)
              .param("time", time).param("dtInSeconds", dtInSeconds)
              .timeout(config.getInt("restServer.connectionTimeoutMs"), config.getInt("restServer.readTimeoutMs"))
              .asString
            RouteDirectives.complete(HttpEntity(APIGatewayResponse.body))
          }
        }
      }
    }
}
