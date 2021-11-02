package grpc

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.pki.pem.{DERPrivateKeyLoader, PEMDecoder}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}

// Skeleton implementation reference: https://developer.lightbend.com/guides/akka-grpc-quickstart-scala/

/**
 * Objects that implements a GRPC Server, that handles the calls from the Client to find logs.
 */
object GRPCServer {
  val logger: Logger = LoggerFactory.getLogger(classOf[grpc.GRPCServer.type])

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "LogFinderServer", conf)
    new GRPCServer(system).run()
  }
}

class GRPCServer(system: ActorSystem[_]) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    val config: Config = ConfigFactory.load()

    val service: HttpRequest => Future[HttpResponse] =
      LogFinderServiceHandler(new LogFinderServiceImpl(system))

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "127.0.0.1", port = config.getInt("akka.grpc.server.port"))
      //.enableHttps(serverHttpContext)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        GRPCServer.logger.info("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        GRPCServer.logger.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }

  /**
   * Defines HTTPS Connection Context
   */
  private def serverHttpContext: HttpsConnectionContext = {
    val privateKey =
      DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem()))
    val fact = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(
      classOf[GRPCServer].getResourceAsStream("/certs/server1.pem")
    )
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry(
      "private",
      privateKey,
      new Array[Char](0),
      Array[Certificate](cer)
    )
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    ConnectionContext.https(context)
  }

  private def readPrivateKeyPem(): String =
    Source.fromResource("certs/server1.key").mkString
}
