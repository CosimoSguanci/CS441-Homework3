package rest.actors

import akka.actor.Actor
import org.slf4j.{Logger, LoggerFactory}

object LogFinderActor {
  case object Get
}

/**
 * Defines endpoints for the LogFinder
 */
class LogFinderActor extends Actor {

  import LogFinderActor._

  private val logger: Logger = LoggerFactory.getLogger(classOf[LogFinderActor.type])

  override def receive: Receive = {
    case Get => logger.info("[LogFinderActor] GET Request Received...")
  }
}