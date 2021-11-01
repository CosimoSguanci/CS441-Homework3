package actors

import akka.actor.Actor

object LogFinderActor {
  case object Get
}

class LogFinderActor extends Actor {

  import LogFinderActor._

  override def receive: Receive = {
    case Get => println("[LogFinderActor] GET Request Received...")
  }
}