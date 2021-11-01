package grpc

//#import
import scala.concurrent.Future

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.MergeHub
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

//#import

//#service-request-reply
//#service-stream
class LogFinderServiceImpl(system: ActorSystem[_]) extends LogFinderService {
  private implicit val sys: ActorSystem[_] = system

/*  //#service-request-reply
  val (inboundHub: Sink[FindLogRequest, NotUsed], outboundHub: Source[FindLogReply, NotUsed]) =
    MergeHub.source[FindLogRequest]
    .map(request => FindLogReply(s"Hello, ${request.time}"))
      .toMat(BroadcastHub.sink[FindLogReply])(Keep.both)
      .run()
  //#service-request-reply*/

  override def findLog(request: FindLogRequest): Future[FindLogReply] = {
    Future.successful(FindLogReply(s"Hello, ${request.time}"))
  }
}
//#service-stream
//#service-request-reply
