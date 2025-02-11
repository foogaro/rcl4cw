package com.foogaro.aws.cloud.watch.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.foogaro.aws.cloud.watch.CloudWatchAPI;
import com.foogaro.aws.cloud.watch.CloudWatchAPI.PutLogsResult;
import com.foogaro.redis.cloud.logs.RedisCloudAPI;
import software.amazon.awssdk.regions.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogSyncHandler implements RequestHandler<ScheduledEvent, Map<String, Object>> {

//    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int DEFAULT_REDIS_CLOUD_BATCH_SIZE = 100;


    @Override
    public Map<String, Object> handleRequest(ScheduledEvent event, Context context) {
        String awsRegion = System.getenv("AWS_REGION");
        if (awsRegion == null) {
            throw new IllegalStateException("AWS_REGION environment variable is not set");
        }

        try {
            RedisCloudAPI redisCloudAPI = new RedisCloudAPI(
                    "https://api.redislabs.com/v1",
                    getRequiredEnv("REDIS_CLOUD_API_KEY"),
                    getRequiredEnv("REDIS_CLOUD_API_SECRET")
            );

            CloudWatchAPI cloudWatchAPI = new CloudWatchAPI(
                    getRequiredEnv("REDIS_CLOUD_LOG_GROUP"),
                    getRequiredEnv("REDIS_CLOUD_LOG_STREAM"),
                    Region.of(awsRegion)
            );

            // Get the last log ID from CloudWatch
            String lastLogId = cloudWatchAPI.getLastRedisCloudLogId();
//            logger.info("Last log ID from CloudWatch: {}", lastLogId);
            System.out.println(String.format("Last log ID from CloudWatch: %s", lastLogId));

            int totalSynced = syncLogsToCloudWatch(redisCloudAPI, cloudWatchAPI, getRequiredEnv("REDIS_CLOUD_BATCH_SIZE", DEFAULT_REDIS_CLOUD_BATCH_SIZE), lastLogId);

//            logger.info("Successfully synced {} logs to CloudWatch in region {}", totalSynced, awsRegion);
            System.out.println(String.format("Successfully synced %d logs to CloudWatch in region %s", totalSynced, awsRegion));

            return Map.of(
                    "statusCode", 200,
                    "body", Map.of(
                            "message", String.format("Successfully synced %d logs", totalSynced),
                            "region", awsRegion,
                            "lastLogId", lastLogId != null ? lastLogId : "none"
                    )
            );

        } catch (Exception e) {
            System.err.println(String.format("Error during log sync: %s", e));
            return Map.of(
                    "statusCode", 500,
                    "body", Map.of(
                            "error", e.getMessage(),
                            "region", awsRegion
                    )
            );
        }
    }

    private int syncLogsToCloudWatch(
            RedisCloudAPI redisCloudAPI,
            CloudWatchAPI cloudWatchAPI,
            int batchSize,
            String stopAtId
    ) {
        return syncLogsToCloudWatch(redisCloudAPI, cloudWatchAPI, batchSize, stopAtId, 0, 0);
    }

    private int syncLogsToCloudWatch(
            RedisCloudAPI redisCloudAPI,
            CloudWatchAPI cloudWatchAPI,
            int batchSize,
            String stopAtId,
            int offset,
            int totalProcessed
    ) {
        try {
            List<Map<String, Object>> currentBatch = new ArrayList<>();
            Stream<Map<String, Object>> logStream = redisCloudAPI.fetchLogs(batchSize, offset, stopAtId);
            List<Map<String, Object>> logs = logStream.collect(Collectors.toList());

            // Base case: no more logs to process
            if (logs.isEmpty()) {
                return totalProcessed;
            }

            // Process current batch
            for (Map<String, Object> log : logs) {
                currentBatch.add(log);

                if (currentBatch.size() >= batchSize) {
                    PutLogsResult result = cloudWatchAPI.putLogEvents(new ArrayList<>(currentBatch));
                    if (!result.success()) {
                        throw new RuntimeException("Failed to send logs to CloudWatch");
                    }
                    totalProcessed += result.count();
                    currentBatch.clear();
                }
            }

            // Handle remaining logs in the batch
            if (!currentBatch.isEmpty()) {
                PutLogsResult result = cloudWatchAPI.putLogEvents(new ArrayList<>(currentBatch));
                if (!result.success()) {
                    throw new RuntimeException("Failed to send final batch to CloudWatch");
                }
                totalProcessed += result.count();
            }

            // Recursive call with updated offset
            return syncLogsToCloudWatch(
                    redisCloudAPI,
                    cloudWatchAPI,
                    batchSize,
                    stopAtId,
                    offset + logs.size(),
                    totalProcessed
            );

        } catch (Exception e) {
            System.err.println(String.format("Error in syncLogsToCloudWatch: %s", e.getMessage()));
            throw new RuntimeException("Failed to sync logs to CloudWatch", e);
        }
    }

    private String getRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    String.format("%s environment variable is not set", name)
            );
        }
        return value;
    }

    private int getRequiredEnv(String name, Integer defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) return Integer.parseInt(value);
        if (defaultValue != null) return defaultValue;
        throw new IllegalStateException(String.format("%s environment variable is not set", name));
    }
}
