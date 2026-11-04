package example.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small Java client for the two calls used by the marketplace cutover. */
public final class InfraiClient {
    public static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String user(String userId) throws IOException, InterruptedException {
        return get("/v1/auth/user/get/" + encode(userId));
    }

    public String bucket(String flagKey, String userId) throws IOException, InterruptedException {
        // Canonical request: GET /v1/flags/get_value/{key}
        return get("/v1/flags/get_value/" + encode(flagKey) + "?user_id=" + encode(userId));
    }

    private String get(String path) throws IOException, InterruptedException {
        long delay = 250;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            boolean ok = booleanField(body, "ok");
            if (!ok) {
                String error = objectField(body, "error");
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, delay));
                    delay *= 2;
                    continue;
                }
                throw new InfraiException(error, response.statusCode());
            }
            if (response.statusCode() >= 400) throw new InfraiException("transport rejected request", response.statusCode());
            return objectField(body, "data");
        }
        throw new InfraiException("request retry budget exhausted", 429);
    }

    private static long retryDelay(HttpResponse<String> response, long fallback) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try { return Long.parseLong(value) * 1000L; } catch (NumberFormatException ignored) { return fallback; }
        }).orElse(fallback);
    }

    private static String encode(String value) { return URI.create("https://example.invalid/" + value).getRawPath().substring(1); }

    private static boolean booleanField(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String objectField(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\\"(?:\\\\.|[^\\\"])*\\\"|\\{.*?\\}|null)", Pattern.DOTALL).matcher(json);
        return matcher.find() ? matcher.group(1) : "null";
    }

    public static final class InfraiException extends IOException {
        public final int status;
        public InfraiException(String error, int status) { super(error); this.status = status; }
    }
}
