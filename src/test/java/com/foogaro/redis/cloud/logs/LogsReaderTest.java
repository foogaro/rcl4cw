//package com.foogaro.redis.cloud.logs;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class LogsReaderTest {
//
//    private final Logger logger = LoggerFactory.getLogger(getClass());
//
//    private RedisCloudAPI logsReader;
//    private final String API_KEY = System.getenv("REDIS_CLOUD_API_KEY");
//    private final String API_SECRET = System.getenv("REDIS_CLOUD_API_SECRET");
//    private final String BASE_URL = "https://api.redislabs.com/v1";
//
//    @BeforeEach
//    void setUp() {
//        assertNotNull(API_KEY, "REDIS_CLOUD_API_KEY environment variable is not set");
//        assertNotNull(API_SECRET, "REDIS_CLOUD_API_SECRET environment variable is not set");
//
//        logsReader = new RedisCloudAPI(BASE_URL, API_KEY, API_SECRET);
//    }
//
//    @Test
//    @DisplayName("Should fetch logs successfully")
//    void testFetchLogs() {
//        CompletableFuture<List<Map<String, Object>>> logsFuture = logsReader.fetchLogs(10, 0, null)
//                .thenApply(stream -> stream.collect(Collectors.toList()));
//
//        List<Map<String, Object>> logs = logsFuture.join();
//
//        assertNotNull(logs, "Logs should not be null");
//        assertTrue(logs.size() > 0, "Should fetch at least one log");
//
//        Map<String, Object> firstLog = logs.get(0);
//        logger.info("First log entry: {}", firstLog);
////        {id=79235299, time=2025-01-31T14:27:45Z, originator=Yoseph Nandana, type=PackageDownload, description=Downloaded RedisInsight for macOS M1 latest  }
//
//        assertNotNull(firstLog.get("id"), "Log should have an ID");
//        assertNotNull(firstLog.get("time"), "Log should have a timestamp");
//        assertNotNull(firstLog.get("type"), "Log should have a type");
//
//    }
//
//    @Test
//    @DisplayName("Should stop at specified log ID")
//    void testFetchLogsWithStopId() {
//        CompletableFuture<List<Map<String, Object>>> initialLogsFuture = logsReader.fetchLogs(3, 0, null)
//                .thenApply(stream -> stream.collect(Collectors.toList()));
//
//        List<Map<String, Object>> initialLogs = initialLogsFuture.join();
//        assertFalse(initialLogs.isEmpty(), "Should fetch initial logs");
//        assertEquals(3, initialLogs.size(), "Should fetch 3 logs");
//        logger.info("initialLogs size: {}", initialLogs.size());
//
//        Map<String, Object> log1 = initialLogs.get(0);
//        assertNotNull(log1, "log1 should not be null!");
//        logger.info("log1 entry: {}", log1);
//        Map<String, Object> log2 = initialLogs.get(1);
//        assertNotNull(log2, "log2 should not be null!");
//        logger.info("log2 entry: {}", log2);
//        Map<String, Object> log3 = initialLogs.get(2);
//        assertNotNull(log3, "log3 should not be null!");
//        logger.info("log3 entry: {}", log3);
//
//        String stopId = initialLogs.get(1).get("id").toString();
//        logger.info("log stopId: {}", stopId);
//
//        CompletableFuture<List<Map<String, Object>>> limitedLogsFuture = logsReader.fetchLogs(10, 0, stopId)
//                .thenApply(stream -> stream.collect(Collectors.toList()));
//
//        List<Map<String, Object>> limitedLogs = limitedLogsFuture.join();
//
//        assertEquals(1, limitedLogs.size(), "Should fetch logs up to stop ID, so only the first one.");
//        logger.info("limitedLogs size: {}", limitedLogs.size());
//        Map<String, Object> limitedLog = limitedLogs.get(0);
//        assertNotNull(limitedLog, "limitedLog should not be null!");
//        logger.info("limitedLog entry: {}", limitedLog);
//    }
//
//}
