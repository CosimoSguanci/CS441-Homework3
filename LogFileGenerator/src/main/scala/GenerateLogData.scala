/*
 *
 *  Copyright (c) 2021. Mark Grechanik and Lone Star Consulting, Inc. All rights reserved.
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under
 *   the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 *
 */
import Generation.{LogMsgSimulator, RandomStringGenerator}
import HelperUtils.{CreateLogger, ObtainConfigReference, Parameters}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}

import collection.JavaConverters.*
import scala.concurrent.{Await, Future, duration}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{Failure, Success, Try}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.apache.commons.io.FileUtils

object GenerateLogData:
  val logger = CreateLogger(classOf[GenerateLogData.type])

//this is the main starting point for the log generator
@main def runLogGenerator =
  import Generation.RSGStateMachine.*
  import Generation.*
  import HelperUtils.Parameters.*
  import GenerateLogData.*

  logger.info("Log data generator started...")
  val INITSTRING = "Starting the string generation"
  val init = unit(INITSTRING)

  val config = ObtainConfigReference("randomLogGenerator") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  val awsCredentials: BasicAWSCredentials = new BasicAWSCredentials(config.getString("randomLogGenerator.awsAccessKey"), config.getString("randomLogGenerator.awsSecretKey"))
  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).withRegion(Regions.US_EAST_2).build()
  val bucketName = config.getString("randomLogGenerator.s3BucketName")
  val key = config.getString("randomLogGenerator.s3ObjectKey")

  val logFuture = Future {
    LogMsgSimulator(init(RandomStringGenerator((Parameters.minStringLength, Parameters.maxStringLength), Parameters.randomSeed)), Parameters.maxCount)
  }

  Try(Await.result(logFuture, Parameters.runDurationInMinutes)) match {
    case Success(value) => {
      logger.info(s"Log data generation has completed after generating ${Parameters.maxCount} records.")

      // Now I upload the log file to S3 and I delete the local file to avoid wasting resources

      try {
        val getRequest: GetObjectRequest = new GetObjectRequest(bucketName, key)
        val S3Object: S3Object = s3Client.getObject(getRequest)
        val objectData: java.io.InputStream = S3Object.getObjectContent() // existing log file in S3
        addLogToS3(objectData)
      } catch {
        case ex: AmazonS3Exception => {
          // This means that there are no log files in S3 bucket
          initLogS3()
        }
      }
    }
    case Failure(exception) => logger.info(s"Log data generation has completed within the allocated time, ${Parameters.runDurationInMinutes}")
  }

  /**
   * Called to upload a log file to AWS S3 for the first time (no object with the key "log" was found)
   */
  def initLogS3() = {
    val dirName = "log"
    val dir = new java.io.File(dirName)
    val fileName = if(dir.exists && dir.isDirectory) dir.listFiles.filter(_.isFile).toList(0).getName else null
    val filePath = s"$dirName/$fileName"
    val newLogFile = new java.io.File(filePath)

    val request: PutObjectRequest = new PutObjectRequest(bucketName, key, newLogFile)
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType("plain/text")
    request.setMetadata(metadata)
    s3Client.putObject(request)

    newLogFile.delete()
  }

  /**
   * Called to add logs to the already exisiting log object on AWS S3
   * @param previousObjectData the InputStream related to the logs already stored in S3. Its content will be merged with the new logs, and the new log object will be uploaded
   */
  def addLogToS3(previousObjectData: java.io.InputStream) = {
    val logFile = new java.io.File("log/log.tmp")
    FileUtils.copyInputStreamToFile(previousObjectData, logFile)

    val dirName = "log"
    val dir = new java.io.File(dirName)
    val fileName = if(dir.exists && dir.isDirectory) dir.listFiles.filter(_.isFile).toList(0).getName else null
    val filePath = s"$dirName/$fileName"
    val newLogFile = new java.io.File(filePath)

    // we need to merge logFile and newLogFile
    val oldContent: String = FileUtils.readFileToString(logFile, "UTF-8")
    val newContent: String = FileUtils.readFileToString(newLogFile, "UTF-8")

    FileUtils.write(logFile, oldContent, "UTF-8")
    FileUtils.write(logFile, newContent, "UTF-8", true)

    val request: PutObjectRequest = new PutObjectRequest(bucketName, key, logFile)
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType("plain/text")
    request.setMetadata(metadata)
    s3Client.putObject(request)

    logFile.delete()
    newLogFile.delete()
  }

