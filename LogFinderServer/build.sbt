name := "LogFinderServer"

version := "0.1"

scalaVersion := "2.13.4"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val generexVersion = "1.0.2"
val akkaActorVersion = "2.5.26"
val akkaHttpVersion = "10.2.6"
val akkaStreamVersion = "2.6.17"
val akkaHttpSprayJsonVersion = "10.2.6"
val scalajVersion = "2.4.2"

lazy val akkaGrpcVersion = "2.1.0"

enablePlugins(AkkaGrpcPlugin)

resolvers += Resolver.jcenterRepo

/*
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
  "com.typesafe.akka" %% "akka-actor" % akkaActorVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpSprayJsonVersion,
  "org.scalaj" %% "scalaj-http" % scalajVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-pki" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaStreamVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test,
)

assemblyJarName in assembly := "server.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}*/

lazy val root = (project in file(".")).
  settings(
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
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
      "com.typesafe.akka" %% "akka-actor" % akkaActorVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpSprayJsonVersion,
      "org.scalaj" %% "scalaj-http" % scalajVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaStreamVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaStreamVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaStreamVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaStreamVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test,
    ),
    assemblyJarName := "LogFinderServer.jar",
  )

assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

