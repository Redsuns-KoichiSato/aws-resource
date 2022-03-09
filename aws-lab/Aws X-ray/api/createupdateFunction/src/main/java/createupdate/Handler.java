package createupdate;

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
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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
    var subsegment = AWSXRay.beginSubsegment("Create/Update");
    APIGatewayProxyResponseEvent response;
    try {
      logger.info("environment: {}", gson.toJson(System.getenv()));
      logger.info("event: {}", gson.toJson(event));

      var client = DynamoDbAsyncClient.builder()
              .overrideConfiguration(ClientOverrideConfiguration.builder()
                      .addExecutionInterceptor(new TracingInterceptor())
                      .build())
              .build();
              
      var tableName = System.getenv("TABLE_NAME");
      var attributes = new HashMap<String, AttributeValue>();
      var userName = getUserFromEvent(event);
      subsegment.putAnnotation("UserId", userName);

      logger.info("body: {}", gson.toJson(event.getBody()));
      var note = gson.fromJson(event.getBody(), Note.class);
      attributes.put("UserId", AttributeValue.builder().s(userName).build());
      attributes.put("NoteId", AttributeValue.builder().n(note.getNoteId()).build());
      attributes.put("Note", AttributeValue.builder().s(note.getNote()).build());

      var request = PutItemRequest.builder()
              .tableName(tableName)
              .item(attributes)
              .build();

      client.putItem(request).join();
      response = new APIGatewayProxyResponseEvent()
              .withStatusCode(200)
              .withHeaders(headers)
              .withIsBase64Encoded(false)
              .withBody(note.getNoteId());
              
    }catch (Exception e) {
      subsegment.addException(e);
      throw e;
    }finally {
      AWSXRay.endSubsegment();
    }
    return response;
  }
}