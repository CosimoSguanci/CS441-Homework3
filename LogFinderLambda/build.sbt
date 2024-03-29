name := "LogFinderLambda"

version := "0.1"

scalaVersion := "3.0.2"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val generexVersion = "1.0.2"
val awsJavaSdkS3Version = "1.12.99"
val awsLambdaJavaCoreVersion = "1.2.1"
val awsLambdaJavaEventsVersion = "3.10.0"
val json4sVersion = "4.0.3"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.slf4j" % "slf4j-api" % sfl4sVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "commons-io" % "commons-io" % apacheCommonIOVersion,
  "org.scalactic" %% "scalactic" % scalacticVersion,
  "org.scalatest" %% "scalatest" % scalacticVersion % Test,
  "org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.github.mifmif" % "generex" % generexVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsJavaSdkS3Version,
  "com.amazonaws" % "aws-lambda-java-core" % awsLambdaJavaCoreVersion,
  "com.amazonaws" % "aws-lambda-java-events" % awsLambdaJavaEventsVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion
)

assemblyJarName in assembly := "LogFinderLambda.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}