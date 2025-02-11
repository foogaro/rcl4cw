//package com.foogaro.aws.cloud.watch;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import software.amazon.awssdk.regions.Region;
//
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class LogsWriterTest {
//
//    private final Logger logger = LoggerFactory.getLogger(getClass());
//
//    private CloudWatchAPI logsWriter;
//    private String logId;
//    private final String CLOUDWATCH_LOG_GROUP = System.getenv("CLOUDWATCH_LOG_GROUP");
//    private final String CLOUDWATCH_LOG_STREAM = System.getenv("CLOUDWATCH_LOG_STREAM");
//    private final String CLOUDWATCH_REGION = System.getenv("CLOUDWATCH_REGION");
//
//    @BeforeEach
//    void setUp() {
//        logsWriter = new CloudWatchAPI(CLOUDWATCH_LOG_GROUP, CLOUDWATCH_LOG_STREAM, Region.of(CLOUDWATCH_REGION));
//    }
//
//    @Test
//    void putLogs_WithValidLogs_ReturnsTrue() {
//        Map<String, Object> logEntry = new HashMap<>();
//        String now = Instant.now().toString();
//        logId = "22101978-" + now;
//        logEntry.put("time", now);
//        logEntry.put("type", "INFO");
//        logEntry.put("originator", "test");
//        logEntry.put("description", "test message");
//        logEntry.put("id", logId);
//        logger.info("Log entry: {}", logEntry);
//
//        CompletableFuture<Boolean> future = logsWriter.putLogs(Collections.singletonList(logEntry));
//
//        Boolean result = assertDoesNotThrow(() -> future.get());
//        assertTrue(result);
//        logger.info("putLogs result: {}", result);
//
//    }
//
//    @Test
//    void getLastLogId_WithValidLog_ReturnsId() {
//        CompletableFuture<String> future = logsWriter.getLastLogId();
//
//        String result = assertDoesNotThrow(() -> future.get());
//        assertNotNull(result);
//        assertEquals(logId, result);
//
//    }
//}