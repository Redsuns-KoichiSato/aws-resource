package delete;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.strategy.sampling.AllSamplingStrategy;
import com.amazonaws.xray.entities.Subsegment;

// TODO 1: Add an AWS Xray library that can be used to instrument the DynamoDB client.
import com.amazonaws.xray.interceptors.TracingInterceptor;
// End TODO 1

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>{
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final HashMap<String, String> headers = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static{
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();
        builder.withSamplingStrategy(new AllSamplingStrategy());
        AWSXRay.setGlobalRecorder(builder.build());
        
    }

    private String getUserFromEvent(APIGatewayProxyRequestEvent event) {
        var userName = "unknown";
        var authorizer = event.getRequestContext().getAuthorizer();
        if (authorizer == null){
            logger.warn("null authorizer - cannot determine username");
            return userName;
        }
        var claims = (Map<String, String>)authorizer.get("claims");
        if (claims == null){
            logger.warn("no JWT in authorizer - cannot determine username");
            return userName;
        }
        return claims.get("cognito:username");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        var subsegment = AWSXRay.beginSubsegment("Delete");
        APIGatewayProxyResponseEvent response;
        try {
            logger.info("environment: {}", gson.toJson(System.getenv()));
    
            var id = event.getPathParameters().get("id");
            var UserId = getUserFromEvent(event);

            logger.info("deleting id: {}", id);

            var client = DynamoDbAsyncClient.builder()
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
// TODO 2: Send calls by this client to AWS X-Ray
                            .addExecutionInterceptor(new TracingInterceptor())
// End TODO 2
                            .build())
                    .build();

            var tableName = System.getenv("TABLE_NAME");
            var key = new HashMap<String, AttributeValue>();
            key.put("UserId", AttributeValue.builder().s(getUserFromEvent(event)).build());
            key.put("NoteId", AttributeValue.builder().n(id).build());
            
// TODO 3                                      
            Subsegment currentSubsegment = AWSXRay.getCurrentSubsegment();
            currentSubsegment.putAnnotation("UserId", UserId);
            currentSubsegment.putAnnotation("NoteId", id);
// TODO 3

            var request = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            client.deleteItem(request).join();
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withIsBase64Encoded(false)
                    .withBody(id);

        }catch (Exception e) {
            subsegment.addException(e);
            throw e;
        }finally {
            logger.warn(" ");
            AWSXRay.endSubsegment();
        }
        return response;
    }
}
