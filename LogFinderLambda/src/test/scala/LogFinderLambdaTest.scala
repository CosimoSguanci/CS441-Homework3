package lambda

import HelperUtils.ObtainConfigReference
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.json4s.*
import org.json4s.jackson.JsonMethods.*

import java.time.LocalTime

class LogFinderLambdaTest extends AnyFlatSpec with Matchers {
  it should "correctly builds JSON Strings with status code 200" in {
    val vector: Vector[String] = Vector("test")
    val correctMD5: String = "098f6bcd4621d373cade4e832627b4f6"

    val jsonString: String = LogFinderLambda.buildJsonString(vector, 200, " ")

    implicit val formats: DefaultFormats.type = DefaultFormats

    val jsonMap: JObject = parse(jsonString).asInstanceOf[JObject]
    jsonMap.values.get("result_md5").get shouldBe correctMD5
  }

  it should "correctly builds JSON Strings with status code 404" in {
    val vector: Vector[String] = Vector.empty

    val jsonString: String = LogFinderLambda.buildJsonString(vector, 404, " ")

    implicit val formats: DefaultFormats.type = DefaultFormats

    val jsonMap: JObject = parse(jsonString).asInstanceOf[JObject]
    jsonMap.values.get("error").get shouldBe "not found"
  }

  it should "correctly locate a log that is in the specified time interval" in {
    val logs: Vector[String] = Vector(
      "11:44:27.098 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0V5hF8uW8jag3",
      "11:44:27.200 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:28.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:29.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:29.565 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.211 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3.cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.809 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - B5tce1Q7hQ9kC5uaf3Q8gG7nbf3E8lce1J8qce3",
      "11:44:31.444 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf,,2ag0ag0J8jZ7jR7nZ8k",
      "11:44:32.234 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k"
    )

    val time: LocalTime = LocalTime.parse("11:44:31")
    val dtInSeconds: Long = 1

    val config = ObtainConfigReference("randomLogGenerator") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    val results: Vector[String] = LogFinderLambda.binarySearch(time, dtInSeconds, logs, config)

    results.length shouldBe 1
    results(0) shouldBe "11:44:30.809 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - B5tce1Q7hQ9kC5uaf3Q8gG7nbf3E8lce1J8qce3"
  }

  it should "correctly return an empty vector if no logs are found" in {
    val logs: Vector[String] = Vector(
      "11:44:27.098 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0V5hF8uW8jag3",
      "11:44:27.200 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:28.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:29.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:29.565 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.211 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3.cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.809 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - B5tce1Q7hQ9kC5uaf3Q8gG7nbf3E8lce1J8qce3.",
      "11:44:31.444 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf,,2ag0ag0J8jZ7jR7nZ8k",
      "11:44:32.234 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k"
    )

    val time: LocalTime = LocalTime.parse("11:44:31")
    val dtInSeconds: Long = 1

    val config = ObtainConfigReference("randomLogGenerator") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    val results: Vector[String] = LogFinderLambda.binarySearch(time, dtInSeconds, logs, config)

    results.length shouldBe 0
  }

  it should "correctly return an empty vector if the specified time interval isn't in logs" in {
    val logs: Vector[String] = Vector(
      "11:44:27.098 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0V5hF8uW8jag3",
      "11:44:27.200 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:28.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - cg1be3be0bg2ae3J8vX7nO7gaf0..V5hF8uW8jag3",
      "11:44:29.241 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:29.565 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.211 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3.cf2ag0ag0J8jZ7jR7nZ8k",
      "11:44:30.809 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - B5tce1Q7hQ9kC5uaf3Q8gG7nbf3E8lce1J8qce3",
      "11:44:31.444 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf,,2ag0ag0J8jZ7jR7nZ8k",
      "11:44:32.234 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - bf2A6vae3ag3ag3cf2ag0ag0J8jZ7jR7nZ8k"
    )

    val config = ObtainConfigReference("randomLogGenerator") match {
      case Some(value) => value
      case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
    }

    val time1: LocalTime = LocalTime.parse("11:50:31")
    val dtInSeconds: Long = 1

    val results1: Vector[String] = LogFinderLambda.binarySearch(time1, dtInSeconds, logs, config)

    results1.length shouldBe 0

    val time2: LocalTime = LocalTime.parse("11:30:31")

    val results2: Vector[String] = LogFinderLambda.binarySearch(time2, dtInSeconds, logs, config)

    results2.length shouldBe 0
  }
}