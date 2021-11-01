import java.util.concurrent.TimeUnit
import akka.actor.{ActorRef, ActorSystem, Props, TypedActor}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{delete, get, parameters, path, post, put, withRequestTimeout}
import akka.http.scaladsl.server.directives.{PathDirectives, RouteDirectives}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.pattern.Patterns
import actors.LogFinderActor
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}


class RouteConfig(implicit val logFinderActorRef: ActorRef,
                  implicit val system: ActorSystem) {

  val APIGatewayURL = "http://localhost:3000/logfinder"

  val getRoute: Route =

    parameters("time", "dtInSeconds") { (time, dtInSeconds) =>
      withRequestTimeout(30.seconds) {
        PathDirectives.pathPrefix("findlogs"){
          get {
            val APIGatewayResponse: HttpResponse[String] = Http(APIGatewayURL).param("time", time).param("dtInSeconds", dtInSeconds).timeout(15000, 30000).asString
            RouteDirectives.complete(HttpEntity(APIGatewayResponse.body))
          }
        }
      }
    }
}
