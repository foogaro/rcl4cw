package com.foogaro.redis.cloud.logs;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RedisCloudAPI {

//    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final Map<String, String> headers;
    private final HttpClient httpClient;

    public RedisCloudAPI(String baseUrl, String apiKey, String apiSecretKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.headers = Map.of(
                "Accept", "application/json",
                "x-api-key", apiKey,
                "x-api-secret-key", apiSecretKey
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Stream<Map<String, Object>> fetchLogs(int batchSize, int offset, String lastLogId) {
        try {
            String url = String.format("%s/logs?limit=%d&offset=%d",
                    baseUrl,
                    batchSize,
                    offset
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .headers(headers.entrySet().stream()
                            .map(e -> new String[]{e.getKey(), e.getValue()})
                            .flatMap(Arrays::stream)
                            .toArray(String[]::new))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP error occurred: " + response.statusCode());
            }

            Map<String, Object> parsedResponse = parseResponse(response.body());
            List<Map<String, Object>> entries = (List<Map<String, Object>>) parsedResponse.get("entries");
            
            if (entries == null) {
                return Stream.empty();
            }

            if (lastLogId != null) {
                int stopIndex = -1;
                for (int i = 0; i < entries.size(); i++) {
                    if (lastLogId.equals(entries.get(i).get("id").toString())) {
                        stopIndex = i;
                        break;
                    }
                }

                if (stopIndex != -1) {
                    // Return entries up to but not including the lastLogId
                    return entries.subList(0, stopIndex).stream();
                }
            }

            return entries.stream();

        } catch (Exception e) {
            System.err.println(String.format("Error fetching logs: %s", e));
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            System.err.println(String.format("Error parsing response: %s", e));
            throw new RuntimeException("Error parsing response", e);
        }
    }

}
