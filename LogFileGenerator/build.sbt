import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

name := "LogFileGenerator"

version := "0.1"

scalaVersion := "3.0.2"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val generexVersion = "1.0.2"
val awsJavaSdkS3Version = "1.12.99"

resolvers += Resolver.jcenterRepo

lazy val root = (project in file(".")).
  settings(
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-core" % logbackVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % "runtime",
      "org.slf4j" % "slf4j-api" % sfl4sVersion,
      "com.typesafe" % "config" % typesafeConfigVersion,
      "commons-io" % "commons-io" % apacheCommonIOVersion,
      "org.scalactic" %% "scalactic" % scalacticVersion,
      "org.scalatest" %% "scalatest" % scalacticVersion % Test,
      "org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
      "com.typesafe" % "config" % typesafeConfigVersion,
      "com.github.mifmif" % "generex" % generexVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsJavaSdkS3Version
    ),
    assemblyJarName := "LogFileGenerator.jar",
  )

assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
