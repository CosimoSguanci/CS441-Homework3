import actors.LogFinderActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.PathDirectives.pathPrefix
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

// Skeleton implementation reference: https://vikasontech.github.io/post/scala-rest-api-with-akka-http/
object RESTServer extends App {
  implicit val system: ActorSystem = ActorSystem("web-app")
  implicit val logFinderActorRef: ActorRef = system.actorOf(Props(new LogFinderActor()))

  private val routeConfig = new RouteConfig()
  val routes = {
    pathPrefix("api") {
      concat(
        routeConfig.getRoute
      )
    }
  }

  //val serverFuture = Http().bindAndHandle(routes, "localhost", 8080)
  Http().newServerAt("localhost", 8080).bind(routes)

  println("Server started...")

/*  StdIn.readLine()
  serverFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())*/
}
