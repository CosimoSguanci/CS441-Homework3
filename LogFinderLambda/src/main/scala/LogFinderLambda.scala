package lambda

import HelperUtils.{CreateLogger, ObtainConfigReference}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3Object, S3ObjectInputStream}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.Config
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{Duration, LocalTime}
import scala.collection.JavaConverters.*
import scala.util.matching.Regex

enum Operation:
  case FIND_BEFORE, FIND_AFTER

/**
 * Lambda Function that performs the search in the log file, given a time interval.
 * It performs a binary search to achieve a search in O(logn) time complexity.
 */
object LogFinderLambda {

  /**
   * Case class that represents a response returned to the client
   *
   * @param body       the body of the HTTP response
   * @param headers    the headers of the HTTP response
   * @param statusCode the statusCode of the HTTP response, it can be 200 (OK) or 404 (Not found)
   */
  case class Response(body: String, headers: Map[String, String], statusCode: Int = 200) {
    def javaHeaders: java.util.Map[String, String] = headers.asJava
  }

  /**
   * Handles the API Gateway Request and performs the requested search in logs stored in AWS S3
   *
   * @param requestEvent the API Gateway Request Event that contains the parameters passed by the Client
   * @return the HTTP Response with a JSON response body, that contains either the MD5 hash of the results or an error message.
   */
  def handle(requestEvent: APIGatewayProxyRequestEvent): Response = { // requestEvent: APIGatewayProxyRequestEvent
    val logger = CreateLogger(classOf[LogFinderLambda.type])
    val config = ObtainConfigReference("randomLogGenerator") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    try {
      logger.info("Lambda Function Invoked...")

      val parameters: Map[String, String] = requestEvent.getQueryStringParameters.asScala.toMap

      val time: LocalTime = LocalTime.parse(parameters.get("time").get)
      val dtInSeconds: Long = parameters.get("dtInSeconds").get.toLong

      logger.info(s"Parameters: Time ${parameters.get("time").get}, dt ${parameters.get("dtInSeconds").get} s")

      logger.info("Trying to authenticate on AWS S3...")

      val awsCredentials: BasicAWSCredentials = new BasicAWSCredentials(config.getString("randomLogGenerator.awsAccessKey"), config.getString("randomLogGenerator.awsSecretKey"))
      val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_2).build()
      val bucketName = config.getString("randomLogGenerator.s3BucketName")
      val key = config.getString("randomLogGenerator.s3ObjectKey")
      val getRequest: GetObjectRequest = new GetObjectRequest(bucketName, key)
      val S3Object: S3Object = s3Client.getObject(getRequest)

      logger.info(s"Objects retrieved: the key $key is present in the bucket")

      val objectData: java.io.InputStream = S3Object.getObjectContent()

      // Should use Arrays or Vectors instead of Lists to have direct accesses in O(1)
      var lines: Vector[String] = IOUtils.readLines(objectData, "UTF-8").asScala.toVector
      val results: Vector[String] = binarySearch(time, dtInSeconds, lines, config)

      logger.info(s"Binary Search performed, found ${results.length} results")

      val responseStatusCode = if (results.length > 0) then 200 else 404

      logger.info(s"HTTP Response Status Code: $responseStatusCode")

      val responseJson = buildJsonString(results, responseStatusCode, config.getString("randomLogGenerator.lineSplitter"))
      Response(responseJson, Map("Content-Type" -> "application/json"), statusCode = responseStatusCode)

    } catch {
      case ex: AmazonS3Exception => {
        // This means that there are no log files in S3 bucket

        logger.info("The key was not found in the bucket: no logs have been uploaded to AWS S3")

        val responseStatusCode = 404
        val responseJson = buildJsonString(Vector.empty, responseStatusCode, config.getString("randomLogGenerator.lineSplitter"))
        Response(responseJson, Map("Content-Type" -> "application/json"), statusCode = responseStatusCode)
      }
      case ex: Exception => {
        logger.warn("Caught Exception for Lambda Function Invocation...")
        
        val responseStatusCode = 500
        val responseJson = buildErrorJsonString()
        Response(responseJson, Map("Content-Type" -> "application/json"), statusCode = responseStatusCode)
      }
    }
  }

  /**
   * Builds the JSON string to be used as Response body
   *
   * @param results      the results of the Binary Search, as a Vector
   * @param statusCode   the statusCode of the HTTP response, it can be 200 (OK) or 404 (Not found)
   * @param lineSplitter the character to be used as delimiter to get the various sections of the log line (e.g., the String instance)
   * @return the JSON string to be embedded in the HTTP Response Body
   */
  def buildJsonString(results: Vector[String], statusCode: Int, lineSplitter: String): String = {
    if (statusCode == 404) {
      return "{\"error\": \"not found\"}"
    }

    val concatenatedResults: String = results.map(line => {
      val tokens = line.split(lineSplitter)
      tokens(tokens.length - 1)
    }).mkString("")

    val resultsMD5: String = DigestUtils.md5Hex(concatenatedResults)
    return s"{\"result_md5\": \"$resultsMD5\"}"
  }

  /**
   * Builds the JSON string to be used as Response body in case a malformed input is passed by the Client
   *
   * @return the JSON string to be embedded in the HTTP Response Body
   */
  def buildErrorJsonString(): String = {
    return "{\"error\": \"malformed input\"}"
  }

  /**
   * Performs the Binary Search on the raw log files retrieved from AWS S3
   *
   * @param time        the time passed by the Client
   * @param dtInSeconds the time delta passed by the Client, used to compute the time interval to be considered
   * @param logs        a Vector containing the log lines
   * @param config      configuration instance
   * @return a Vector containing only the lines of the log that are in the specified time interval, and that match the Regex Pattern defined in configuration
   */
  def binarySearch(time: LocalTime, dtInSeconds: Long, logs: Vector[String], config: Config): Vector[String] = {
    val start = time.minusSeconds(dtInSeconds)
    val end = time.plusSeconds(dtInSeconds)

    val length: Int = logs.length
    val lineSplitter: String = config.getString("randomLogGenerator.lineSplitter")
    val firstTimestamp: LocalTime = LocalTime.parse(logs(0).split(lineSplitter)(0))
    val lastTimestamp: LocalTime = LocalTime.parse(logs(length - 1).split(lineSplitter)(0))

    if (end.isBefore(firstTimestamp) || start.isAfter(lastTimestamp)) {
      return Vector.empty
    }

    // The searched timestamp could be in the logs
    val foundLogs: Vector[String] = binarySearchInner(start, end, logs, lineSplitter)

    return foundLogs.filter(log => {
      val tokens = log.split(lineSplitter)
      val stringInstance = tokens(tokens.length - 1)
      val regexPattern = new Regex(config.getString("randomLogGenerator.Pattern"))

      // only return the logs that match the right Regex Pattern
      regexPattern.findFirstIn(stringInstance).getOrElse(null) != null
    })
  }

  /**
   * Inner recursive function that actually performs the binary search in log files
   *
   * @param start        the start of the searched time intervals
   * @param end          the end of the searched time intervals
   * @param logs         a Vector containing the log lines
   * @param lineSplitter the character to be used as delimiter to get the various sections of the log line (e.g., the String instance)
   * @return a Vector containing only the lines of the log that are in the specified time interval
   */
  def binarySearchInner(start: LocalTime, end: LocalTime, logs: Vector[String], lineSplitter: String): Vector[String] = {
    val length: Int = logs.length

    if (length == 0) {
      return Vector.empty
    }

    val middleTime: LocalTime = LocalTime.parse(logs(length / 2).split(lineSplitter)(0))

    val isAfterTheStart: Boolean = middleTime.isAfter(start)
    val isBeforeTheEnd: Boolean = middleTime.isBefore(end)

    if (isAfterTheStart && isBeforeTheEnd) {
      // found time interval
      // we have to collect all the logs that are in the interval

      val beforeLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) - 1, start, end, Operation.FIND_BEFORE, lineSplitter)
      val afterLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) + 1, start, end, Operation.FIND_AFTER, lineSplitter)
      val foundLogs: Vector[String] = beforeLogs ++ Vector(logs(length / 2)) ++ afterLogs

      return foundLogs
    }

    else if (isAfterTheStart) {
      // we take the 1st half
      binarySearchInner(start, end, logs.slice(0, (length / 2) - 1), lineSplitter)
    }
    else {
      // we take the 2nd half
      binarySearchInner(start, end, logs.slice((length / 2) + 1, length), lineSplitter)
    }
  }

  /**
   * Inner recursive function that is called once a log that is in the searched time interval is found. This function is needed to get all the other logs that are
   * in the time interval, both those that are before the first log found and those that are after it in the log file.
   *
   * @param logs         a Vector containing the log lines
   * @param foundLogs    a Vector containing only the lines of the log that are in the specified time interval, updated at every recursive call
   * @param index        current index in the log Vector
   * @param start        the start of the searched time intervals
   * @param end          the end of the searched time intervals
   * @param operation    determines if we have to increase the index or decrease it
   * @param lineSplitter the character to be used as delimiter to get the various sections of the log line (e.g., the String instance)
   * @return the found logs that are in the right time interval before or after the starting index
   */
  def findAllLogsBeforeOrAfter(logs: Vector[String], foundLogs: Vector[String], index: Int, start: LocalTime, end: LocalTime, operation: Operation, lineSplitter: String): Vector[String] = {
    if (index < 0 || index >= logs.length) {
      return foundLogs
    }

    val time: LocalTime = LocalTime.parse(logs(index).split(lineSplitter)(0))

    val isAfterTheStart: Boolean = time.isAfter(start)
    val isBeforeTheEnd: Boolean = time.isBefore(end)

    if (isAfterTheStart && isBeforeTheEnd) {
      val newLog: String = logs(index)
      val newFoundLogs: Vector[String] = foundLogs ++ Vector(newLog)
      val newIndex = if (operation == Operation.FIND_BEFORE) then index - 1 else index + 1
      return findAllLogsBeforeOrAfter(logs, newFoundLogs, newIndex, start, end, operation, lineSplitter)
    }

    return foundLogs
  }
}