package lambda

import HelperUtils.ObtainConfigReference
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object, S3ObjectInputStream}
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters.*
import java.io.File
import java.time.{Duration, LocalTime}

enum Operation:
  case FIND_BEFORE, FIND_AFTER

object LogFinderLambda {

  case class Response(body: String, headers: Map[String,String], statusCode: Int = 200) {
    def javaHeaders: java.util.Map[String, String] = headers.asJava
  }

  def handle(requestEvent: APIGatewayProxyRequestEvent) : Response = {

    val parameters: Map[String, String] = requestEvent.getQueryStringParameters.asScala.toMap

    //val time = LocalTime.parse("01:19:10")
    //val dtInSeconds = 10

    val time: LocalTime = LocalTime.parse(parameters.get("time").get)
    val dtInSeconds: Long = parameters.get("dtInSeconds").get.toLong

    val config = ObtainConfigReference("randomLogGenerator") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    val awsCredentials: BasicAWSCredentials = new BasicAWSCredentials(config.getString("randomLogGenerator.awsAccessKey"), config.getString("randomLogGenerator.awsSecretKey"))
    val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_2).build()
    val bucketName = config.getString("randomLogGenerator.s3BucketName")
    //val key = "log"
    val key = "LogFileGenerator.2021-10-31.log"
    val getRequest: GetObjectRequest = new GetObjectRequest(bucketName, key)
    val S3Object: S3Object = s3Client.getObject(getRequest)
    val objectData: java.io.InputStream = S3Object.getObjectContent()

    // Should use Arrays or Vectors instead of Lists to have direct accesses in O(1)
    val lines: Vector[String] = IOUtils.readLines(objectData, "UTF-8").asScala.toVector

    val results: Vector[String] = binarySearch(time, dtInSeconds, lines)

    Response(results.mkString(","), Map("Content-Type" -> "text/plain"))

  }

  def binarySearch(time: LocalTime, dtInSeconds: Long, logs: Vector[String]): Vector[String] = {
    val start = time.minusSeconds(dtInSeconds)
    val end = time.plusSeconds(dtInSeconds)

    val length: Int = logs.length
    val firstTimestamp: LocalTime = LocalTime.parse(logs(0).split(" ")(0))
    val lastTimestamp: LocalTime = LocalTime.parse(logs(length - 1).split(" ")(0))

    if(end.isBefore(firstTimestamp) || start.isAfter(lastTimestamp)) {
      return null
    }

    // The searched timestamp could be in the logs
    val foundLogs: Vector[String] = binarySearchInner(start, end, logs)
    return foundLogs
  }

  def binarySearchInner(start: LocalTime, end: LocalTime, logs: Vector[String]): Vector[String] = {
    val length: Int = logs.length

    val middleTime: LocalTime = LocalTime.parse(logs(length / 2).split(" ")(0))

    val isAfter: Boolean = middleTime.isAfter(start)
    val isBefore: Boolean = middleTime.isBefore(end)

    if(isAfter && isBefore) {
      // found time interval
      // we have to collect all the logs that are in the interval

      val beforeLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) - 1, start, end, Operation.FIND_BEFORE)
      val afterLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) + 1, start, end, Operation.FIND_AFTER)

      val foundLogs: Vector[String] = beforeLogs ++ Vector(logs(length / 2)) ++ afterLogs

      return foundLogs
    }

    else if(!isAfter) {
      // we take the 2nd half
      binarySearchInner(start, end, logs.slice((length / 2) + 1, length))
    }
    else {
      // we take the 1st half
      binarySearchInner(start, end, logs.slice(0, (length / 2) - 1))
    }
  }

  def findAllLogsBeforeOrAfter(logs: Vector[String], foundLogs: Vector[String], index: Int, start: LocalTime, end: LocalTime, operation: Operation): Vector[String] = {
    val time: LocalTime = LocalTime.parse(logs(index).split(" ")(0))

    val isAfter: Boolean = time.isAfter(start)
    val isBefore: Boolean = time.isBefore(end)

    if(isAfter && isBefore) {
      val newLog: String = logs(index)
      val newFoundLogs: Vector[String] = foundLogs ++ Vector(newLog)
      val newIndex = if(operation == Operation.FIND_BEFORE) then index - 1 else index + 1
      return findAllLogsBeforeOrAfter(logs, newFoundLogs, newIndex, start, end, operation)
    }

    return foundLogs
  }
}