# Cosimo Sguanci
## csguan2@uic.edu - 3rd Homework (CS441)

For this homework, four separate modules have been developed:
- LogFileGenerator: it generates log data and send this logs to an AWS S3 bucket for other computations to take place. In production, this module is deployed to AWS EC2.
- LogFinderLambda: the Function that makes use of the AWS FaaS (AWS Lambda) to perform a binary search on log files to determine if a log with an associated string instance that respects the configured Regex pattern can be found in logs, given a time interval as input.
- LogFinderServer: implements RESTful and gRPC services that act as a middleware between the client and the deployed Lambda Function.
- LogFinderClient: sample client applications that make calls to the Server (both as REST commands and gRPC calls) to perform logs searches.

## Installation instructions
To build the JARs for the modules developed for this homework, the first step is to install Scala through sbt (installation files available [here](https://www.scala-lang.org/download/scala3.html)).

The next step is to clone the repository with the following command:

(HTTPS)
```
git clone https://github.com/CosimoSguanci/LogFileGenerator.git
```

(SSH)
```
git clone git@github.com:CosimoSguanci/CS441-Homework3.git
```

To start building applications, we access the repository main folder:

```
cd CS441-Homework3
```

### LogFileGenerator
To build the JAR file for the `LogFileGenerator` module:

```
cd LogFileGenerator
sbt assembly
```

### LogFinderLambda
To run tests and the build the JAR file for the `LogFinderLambda` module:

```
cd LogFinderLambda
sbt test
sbt assembly
```

In this case we have to make sure to use Java 8 or Java 11 to compile the application, since AWS Lambda only supports these two versions of the Java runtime. 
For example, to switch to the 1.8 version of the JDK on macOS it's possible to use the following command:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

By default, the JAR will be placed in `target/scala-3.0.2/LogFinderLambda.jar`

### LogFinderServer
To build the JAR file for the `LogFinderServer` module:

```
cd LogFinderServer
sbt assembly
```

By default, the JAR will be placed in `target/scala-2.13/LogFinderServer.jar`

It is possible to also build the JAR file for the `LogFinderClient` module, but for this example we will directly use `sbt` to run the clients and interact with the backend services.

## Instructions for running the developed architecture

For the `LogFileGenerator` and `LogFinderLambda` modules it is necessary to configure the `awsAccessKey` and `awsSecretKey` in `application.conf` configuration file (for both projects) in order to interact with AWS S3.

### LogFileGenerator
The first module to execute is the `LogFileGenerator` in order to upload log files to AWS S3. We can run the corresponding JAR with the following command:

```
java -jar LogFileGenerator.jar
```

The `LogFileGenerator` will start and all the generated logs will be uploaded to the S3 bucket specified in the `application.conf` file.

### LogFinderLambda
To test our Lambda Function locally, we have to install the following required applications:
- AWS SAM CLI (available [here](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html))
- AWS CLI (available [here](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html))
- Docker (available [here](https://docs.docker.com/get-docker/))

To deploy the Lambda Function first we have to start Docker. Then, the following command will start the API Gateway + Lambda service:

```
sam local start-api
```

The API Gateway Proxy will start listening on port 3000 (by default for local environment).

### LogFinderServer

In order to make the servers work in a `localhost` environment, it is necessary to modify the configuration to make them point to the local API Gateway Proxy:

```
restServer {
  ...
  APIGatewayURL = "http://127.0.0.1:3000/logfinder"
  ...
}

akka.grpc.server {
  ...
  APIGatewayURL = "http://127.0.0.1:3000/logfinder"
  ...
}
```

To start the REST service:

```
java -cp LogFinderServer.jar rest.RESTServer
```

To start the gRPC service>

```
java -cp LogFinderServer.jar grpc.GRPCServer
```

### LogFinderClient

By default, in the `application.conf` the endpoints that the clients will try to connect to are configured as the remote endpoints deployed in AWS; to make them point to `localhost` endpoints the following change in configuration is needed:

```
akka.grpc.client {
  "logfinder.LogFinderService" {
    host = "127.0.0.1"
    ...
  }
  ...
}  

restClient {
  endpoint = "http://localhost:8080/api/findlogs"
  ...
}  
```

In order to run the sample client provided with the homework we can make use of `sbt`. All the commands shown below are executed from the `LogFinderClient` project root folder.

#### REST
```
sbt "runMain rest.RESTClient"
```

To test the REST endpoint it is also possible to make use of popular tools like cURL or Postman.

#### gRPC
```
sbt "runMain grpc.LogFinderClient"
```

In order to change the parameters passed to the backend, the following entry can be modified in the `application.conf` file:

```
defaultTime = X 
defaultDtSeconds = Y
```

More details about the implemented architecture can be found in the `doc/DOC.md` file.

Video showing the deployment on AWS: WIP


