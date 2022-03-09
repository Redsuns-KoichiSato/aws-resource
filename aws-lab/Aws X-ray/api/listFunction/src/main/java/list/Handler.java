package list;

import list.Note;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import com.amazonaws.xray.strategy.sampling.AllSamplingStrategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
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
        var subsegment = AWSXRay.beginSubsegment("List");
        APIGatewayProxyResponseEvent response;
        try {
            logger.info("environment: {}", gson.toJson(System.getenv()));
            logger.info("event: {}", gson.toJson(event));

            var client = DynamoDbAsyncClient.builder()
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .addExecutionInterceptor(new TracingInterceptor())
                            .build())
                    .build();
            subsegment.putAnnotation("UserId", getUserFromEvent(event));
      
            var tableName = System.getenv("TABLE_NAME");
            var attributes = new HashMap<String, AttributeValue>();
            attributes.put(":v1", AttributeValue.builder().s(getUserFromEvent(event)).build());
            var request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("UserId = :v1")
                    .expressionAttributeValues(attributes)
                    .build();

            var queryResponse = client.query(request);
            var notes = queryResponse.join().items().stream().map(n -> toNote(n)).toArray();
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withIsBase64Encoded(false)
                    .withBody(gson.toJson(notes));
                
             logger.info("table name: {}", gson.toJson(tableName));    
             logger.info("response: {}", gson.toJson(response));


        }catch (Exception e) {
            subsegment.addException(e);
            throw e;
        }finally {
            AWSXRay.endSubsegment();
        }
        return response;
    }
    
    private Note toNote(Map<String, AttributeValue> item)  {
        var rc = new Note();
        var attrsSet = 0;
        for (var key : item.keySet()) {
            attrsSet++;
            if (key == "Note"){
                rc.setNote(item.get(key).s());
            }else if (key == "NoteId") {
                rc.setNoteId(item.get(key).n());
            }else if (key == "UserId") {
                rc.setUserId(item.get(key).s());
            }else{
                logger.warn("unknown DDB attribute {}", key);
                attrsSet--;
            }
        }
        return rc;
    }
    
}
