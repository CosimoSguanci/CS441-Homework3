package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

// Skeleton implementation reference: https://www.bks2.com/2019/05/02/hello-scala-aws-lambda

/**
 * Handler that forwards API Gateway requests to the Lambda Function, and returns its results (it acts as a proxy)
 */
public class ApiGatewayProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent
                                                              requestEvent, Context context) {

        LogFinderLambda.Response response = LogFinderLambda.handle(requestEvent);

        return new APIGatewayProxyResponseEvent()
                .withBody(response.body())
                .withStatusCode(response.statusCode())
                .withHeaders(response.javaHeaders());
    }
}