package rest

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.directives.PathDirectives.pathPrefix
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import rest.actors.LogFinderActor

// Skeleton implementation reference: https://vikasontech.github.io/post/scala-rest-api-with-akka-http/
object RESTServer extends App {
  implicit val system: ActorSystem = ActorSystem("web-app")
  implicit val logFinderActorRef: ActorRef = system.actorOf(Props(new LogFinderActor()))
  private val routeConfig = new RouteConfig()
  private val logger: Logger = LoggerFactory.getLogger(classOf[RESTServer.type])
  private val config: Config = ConfigFactory.load()

  val routes = {
    pathPrefix("api") {
      concat(
        routeConfig.getRoute
      )
    }
  }

  Http().newServerAt("localhost", config.getInt("restServer.port")).bind(routes)
  logger.info(s"Server started at port ${config.getInt("restServer.port")}...")
}
