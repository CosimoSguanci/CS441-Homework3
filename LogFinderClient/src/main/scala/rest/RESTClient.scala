package rest

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpResponse}

object RESTClient {
  def main(args: Array[String]): Unit = {
    val logger: Logger = LoggerFactory.getLogger(classOf[RESTClient.type])
    val config: Config = ConfigFactory.load()

    val endpointUrl = config.getString("restClient.endpoint")

    val RESTResponse: HttpResponse[String] = Http(endpointUrl)
      .param("time", config.getString("restClient.defaultTime")).param("dtInSeconds", config.getString("restClient.defaultDtSeconds"))
      .timeout(config.getInt("restClient.connectionTimeoutMs"), config.getInt("restClient.readTimeoutMs"))
      .asString

    logger.info(s"[REST] Response from REST Server: ${RESTResponse.body}")
  }
}
