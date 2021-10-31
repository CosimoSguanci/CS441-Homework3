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
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}

import collection.JavaConverters.*
import scala.concurrent.{Await, Future, duration}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.{Failure, Success, Try}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

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

  val logFuture = Future {
    LogMsgSimulator(init(RandomStringGenerator((Parameters.minStringLength, Parameters.maxStringLength), Parameters.randomSeed)), Parameters.maxCount)
  }

  Try(Await.result(logFuture, Parameters.runDurationInMinutes)) match {
    case Success(value) => {
      logger.info(s"Log data generation has completed after generating ${Parameters.maxCount} records.")

      // Now I upload the log file to S3 and I delete the local file to avoid wasting resources

      val dirName = "log"
      val dir = new java.io.File(dirName)
      val fileName = if(dir.exists && dir.isDirectory) dir.listFiles.filter(_.isFile).toList(0).getName else null
      val filePath = s"$dirName/$fileName"
      val file = new java.io.File(filePath)

      val request: PutObjectRequest = new PutObjectRequest(bucketName, fileName, file)
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentType("plain/text")
      request.setMetadata(metadata)
      s3Client.putObject(request)
      file.delete()
    }
    case Failure(exception) => logger.info(s"Log data generation has completed within the allocated time, ${Parameters.runDurationInMinutes}")
  }

