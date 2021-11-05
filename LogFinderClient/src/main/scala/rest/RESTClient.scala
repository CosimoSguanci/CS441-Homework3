package rest

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpResponse}

import java.util


/**
 * Client that performs HTTP requests to the REST Server to find logs in the specified time intervals
 */
object RESTClient {
  def main(args: Array[String]): Unit = {
    val logger: Logger = LoggerFactory.getLogger(classOf[RESTClient.type])
    val config: Config = ConfigFactory.load()

    val endpointUrl = config.getString("restClient.endpoint")

    logger.info(s"[REST] Starting GET request...")

    val GETResponse: HttpResponse[String] = Http(endpointUrl)
      .param("time", config.getString("restClient.defaultTime")).param("dtInSeconds", config.getString("restClient.defaultDtSeconds"))
      .timeout(config.getInt("restClient.connectionTimeoutMs"), config.getInt("restClient.readTimeoutMs"))
      .asString

    logger.info(s"[REST] Response from REST Server (GET): ${GETResponse.body}")

    logger.info(s"[REST] Starting POST request...")

    val postReq = new HttpPost(endpointUrl)
    val nameValuePairs = new util.ArrayList[BasicNameValuePair](util.Arrays.asList(new BasicNameValuePair("time", config.getString("restClient.defaultTime")), new BasicNameValuePair("dtInSeconds", config.getString("restClient.defaultDtSeconds"))))
    postReq.setEntity(new UrlEncodedFormEntity(nameValuePairs))

    val httpClient = HttpClientBuilder.create().build()
    val POSTResponse = httpClient.execute(postReq)
    val POSTResponseString = EntityUtils.toString(POSTResponse.getEntity)

    logger.info(s"[REST] Response from REST Server (POST): $POSTResponseString")
  }
}
