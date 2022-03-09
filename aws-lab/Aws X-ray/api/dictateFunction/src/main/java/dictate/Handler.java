package dictate;

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
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.polly.PollyAsyncClient;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>{
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final HashMap<String, String> headers = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final PollyAsyncClient pollyclient = PollyAsyncClient.builder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .addExecutionInterceptor(new TracingInterceptor())
                    .build())
            .build();

    private static final S3AsyncClient s3client = S3AsyncClient.builder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .addExecutionInterceptor(new TracingInterceptor())
                    .build())
            .build();

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
        var subsegment = AWSXRay.beginSubsegment("Dictate");
        APIGatewayProxyResponseEvent response;
        try {
            logger.info("environment: {}", gson.toJson(System.getenv()));
            logger.info("event: {}", gson.toJson(event));
            var id = event.getPathParameters().get("id");
            var VoiceId = gson.fromJson(event.getBody(), DictateRequest.class).getVoiceId();
            var bucket = getBucket(context);
            logger.info("id: {}", id);
            logger.info("VoiceId: {}", VoiceId);
            logger.info("bucket: {}", bucket);
            subsegment.putAnnotation("userId", getUserFromEvent(event));
            subsegment.putAnnotation("noteId", id);
            subsegment.putAnnotation("bucket", bucket);

            // /notes/{id}/POST
            //
            // This function does the following:
            // 1. Takes a JSON payload from API gateway
            // 2. Calls DynamoDB to fetch the note text from the userId and noteId
            // 3. Calls the Polly synthesize_speech API to convert text to speech
            // 4. Stores the resulting audio in an MP3 file in /tmp
            // 5. Uploads the MP3 file to S3
            // 6. Creates a pre-signed URL for the MP3 file
            // 7. Returns the pre-signed URL to API Gateway

            // 2
            var noteText = getNoteText(getUserFromEvent(event), id);
            logger.info("noteText: {}", noteText);

            // 3/4
            var destfile = Path.of(System.getProperty("java.io.tmpdir"),
                    getUserFromEvent(event) + "-" + id + ".mp3");
            try {
                Files.deleteIfExists(destfile);
            }catch(IOException e) {
                logger.error("could not delete temp file - trying to move ahead anyway");
                // yolo - we'll catch exception later on if this is a problem
            }
            pollyclient.synthesizeSpeech(SynthesizeSpeechRequest.builder()
                    .outputFormat("mp3")
                    .text(noteText)
                    .voiceId(VoiceId)
                    .build(), destfile).join();
            logger.info("synth complete");

            // 5
            var key = getUserFromEvent(event) + "/" + id + ".mp3";
            s3client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("audio/mpeg")
                    .build(),destfile).join();
            logger.info("put object complete");

            // 6
            var presigner = S3Presigner.builder().build();
            var url = presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .build()).url();
            logger.info("generated url");

            // 7
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withIsBase64Encoded(false)
                    .withBody('"' + url.toString() + '"');

        }catch (Exception e) {
            subsegment.addException(e);
            throw e;
        }finally {
            AWSXRay.endSubsegment();
        }
        return response;
    }

    private static String getBucket(Context context) {
        var bucket = System.getenv("MP3_BUCKET_NAME");
        if (bucket != null && !bucket.equals("")) return bucket;
        return context.getClientContext().getEnvironment().get("MP3_BUCKET_NAME");
    }
    private static String getNoteText(String username, String noteId) {
        var client = DynamoDbAsyncClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build())
                .build();

        var tableName = System.getenv("TABLE_NAME");
        var key = new HashMap<String, AttributeValue>();
        key.put("UserId", AttributeValue.builder().s(username).build());
        key.put("NoteId", AttributeValue.builder().n(noteId).build());

        var request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        var queryResponse = client.getItem(request).join();
        return queryResponse.item().get("Note").s();
    }

    private static class DictateRequest {
        private String VoiceId;

        public String getVoiceId() {
            return VoiceId;
        }

        public void setVoiceId(String VoiceId) {
            this.VoiceId = VoiceId;
        }
    }
}
