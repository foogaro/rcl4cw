package com.foogaro.aws.cloud.watch;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class CloudWatchAPI {

//    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CloudWatchLogsClient client;
    private final String logGroup;
    private final String logStream;
    
    public CloudWatchAPI(String logGroup, String logStream, Region region) {
        this.logGroup = logGroup;
        this.logStream = logStream;
        this.client = createClient(region);
    }

    CloudWatchLogsClient createClient(Region region) {
        return CloudWatchLogsClient.builder()
                .region(region)
                .build();
    }

    public void createLogGroup() {
        try {
            CreateLogGroupRequest request = CreateLogGroupRequest.builder()
                    .logGroupName(logGroup)
                    .build();
            
            client.createLogGroup(request);
            System.out.println("Log group created: " + logGroup);
        } catch (ResourceAlreadyExistsException e) {
            System.out.println("Log group already exists: " + logGroup);
        }
    }

    public void createLogStream() {
        try {
            CreateLogStreamRequest request = CreateLogStreamRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .build();
            
                    client.createLogStream(request);
            System.out.println("Log stream created: " + logStream);
        } catch (ResourceAlreadyExistsException e) {
            System.out.println("Log stream already exists: " + logStream);
        }
    }

    public PutLogsResult putLogEvents(List<Map<String, Object>> logs) {
        try {
            // Converti i log in formato JSON
            List<InputLogEvent> logEvents = logs.stream()
                    .map(log -> {
                        try {
                            String jsonMessage = new ObjectMapper().writeValueAsString(log);
                            return InputLogEvent.builder()
                                    .timestamp(System.currentTimeMillis())
                                    .message(jsonMessage)
                                    .build();
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Error converting log to JSON", e);
                        }
                    })
                    .collect(Collectors.toList());

            // Crea la richiesta
            PutLogEventsRequest request = PutLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .logEvents(logEvents)
                    .build();

            // Invia i log
            PutLogEventsResponse response = client.putLogEvents(request);
            System.out.println("Successfully sent " + logs.size() + " logs to CloudWatch");
            return new PutLogsResult(true, logs.size());

        } catch (Exception e) {
            System.err.println("Error putting log events: " + e.getMessage());
            return new PutLogsResult(false, 0);
        }
    }

    private String getSequenceToken() {
        try {
            DescribeLogStreamsRequest request = DescribeLogStreamsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamNamePrefix(logStream)
                    .build();

            DescribeLogStreamsResponse response = client.describeLogStreams(request);
            
            return response.logStreams().stream()
                    .filter(stream -> stream.logStreamName().equals(logStream))
                    .findFirst()
                    .map(LogStream::uploadSequenceToken)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error getting sequence token: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }

    public String getLastRedisCloudLogId() {
        try {
            GetLogEventsResponse response = client.getLogEvents(GetLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .startFromHead(true)
                    .limit(1)
                    .build());
            System.out.println("GetLogEventsResponse: " + response);
    
            List<OutputLogEvent> events = response.events();
            System.out.println("List<OutputLogEvent>: " + events);
            if (events == null || events.isEmpty()) {
                System.out.println("No previous logs found in CloudWatch");
                return null;
            }
    
            OutputLogEvent outputLogEvent = events.getFirst();
            System.out.println("OutputLogEvent: " + outputLogEvent);
            if (outputLogEvent != null) {
                String message = outputLogEvent.message();
                System.out.println("message: " + message);
                System.out.println("outputLogEvent.toString(): " + outputLogEvent.toString());
                System.out.println("outputLogEvent.timestamp(): " + outputLogEvent.timestamp());
                System.out.println("outputLogEvent.sdkFields(): " + outputLogEvent.sdkFields());
                try {
                    // Parse il messaggio JSON
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(message);
                    if (jsonNode.has("id")) {
                        String idStr = jsonNode.get("id").asText();
                        System.out.println(String.format("Found last log ID: %s", idStr));
                        return idStr;
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing JSON message: " + e.getMessage());
                }
            }
    
            System.out.println(String.format("Could not extract ID from last log message: %s", outputLogEvent));
            return null;
    
        } catch (ResourceNotFoundException e) {
            System.out.println(String.format("Log stream does not exist yet: %s", e));
            return null;
        } catch (Exception e) {
            System.err.println(String.format("Error getting last log ID: %s", e));
            throw new RuntimeException("Failed to get last log ID", e);
        }
    }

    private InputLogEvent convertToCloudWatchFormat(Map<String, Object> logEntry) {
        try {
            System.out.println("logEntry: " + logEntry);
            // Parse the ISO 8601 timestamp
            Instant timestamp = Instant.parse((String) logEntry.get("time"));

            // Format the message
            String message = String.format("[%s] %s - %s (ID: %s)",
                    logEntry.get("type"),
                    logEntry.get("originator"),
                    logEntry.get("description"),
                    logEntry.get("id"));
            System.out.println("message: " + message);

            return InputLogEvent.builder()
                    .timestamp(timestamp.toEpochMilli())
                    .message(message)
                    .build();

        } catch (DateTimeParseException e) {
            System.err.println(String.format("Error parsing timestamp: %s", logEntry.get("time"), e));
            throw new RuntimeException("Failed to parse timestamp", e);
        }
    }

    public record PutLogsResult(boolean success, int count) {}

}
