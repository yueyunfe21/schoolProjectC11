package com.bot.dhxy.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class DiagnosticCaseUploaderServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        recordsUploadedRemoteKeysAfterSuccessfulPost();
        recordsPermanentFailureForInvalidTokenOrSchema();
        recordsRetryableFailureForNetworkError();
    }

    private static void recordsUploadedRemoteKeysAfterSuccessfulPost() throws Exception {
        AtomicInteger posts = new AtomicInteger();
        try (TestServer server = TestServer.start(200,
                "{\"ok\":true,\"code\":\"CASE_UPLOADED\",\"message\":\"ok\",\"caseKey\":\"cases/date/machine/task/c.case.json\",\"indexKey\":\"indexes/date.json\"}",
                posts)) {
            Path casePath = writeCase(Files.createTempDirectory("dhxy-case-upload-success"));
            DiagnosticCaseUploaderService service = DiagnosticCaseUploaderService.forTest(
                    true,
                    server.uri("/api/case/upload"),
                    "secret-token",
                    Duration.ofMillis(500),
                    Duration.ofMillis(1000),
                    3,
                    Duration.ofSeconds(1));

            service.uploadCaseOnce(casePath);
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));

            require(posts.get() == 1, "case should be posted exactly once");
            require("UPLOADED".equals(root.path("upload").path("status").asText()),
                    "successful response must mark case uploaded");
            require("cases/date/machine/task/c.case.json".equals(root.path("upload").path("caseKey").asText()),
                    "remote case key must be persisted");
            require("indexes/date.json".equals(root.path("upload").path("indexKey").asText()),
                    "remote index key must be persisted");
            require(root.path("upload").path("attempts").asInt() == 1, "attempt count must increment");
        }
    }

    private static void recordsPermanentFailureForInvalidTokenOrSchema() throws Exception {
        AtomicInteger posts = new AtomicInteger();
        try (TestServer server = TestServer.start(401,
                "{\"ok\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"bad token\"}",
                posts)) {
            Path casePath = writeCase(Files.createTempDirectory("dhxy-case-upload-permanent"));
            DiagnosticCaseUploaderService service = DiagnosticCaseUploaderService.forTest(
                    true,
                    server.uri("/api/case/upload"),
                    "wrong-token",
                    Duration.ofMillis(500),
                    Duration.ofMillis(1000),
                    3,
                    Duration.ofSeconds(1));

            service.uploadCaseOnce(casePath);
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));

            require(posts.get() == 1, "permanent failure still makes one HTTP attempt");
            require("FAILED_PERMANENT".equals(root.path("upload").path("status").asText()),
                    "401/invalid schema style response must be permanent");
            require("UNAUTHORIZED".equals(root.path("upload").path("responseCode").asText()),
                    "server error code must be persisted");
            require(root.path("upload").path("attempts").asInt() == 1, "attempt count must persist");
        }
    }

    private static void recordsRetryableFailureForNetworkError() throws Exception {
        Path casePath = writeCase(Files.createTempDirectory("dhxy-case-upload-network"));
        DiagnosticCaseUploaderService service = DiagnosticCaseUploaderService.forTest(
                true,
                URI.create("http://127.0.0.1:1/api/case/upload"),
                "secret-token",
                Duration.ofMillis(100),
                Duration.ofMillis(100),
                3,
                Duration.ofSeconds(1));

        service.uploadCaseOnce(casePath);
        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(casePath, StandardCharsets.UTF_8));

        require("FAILED_RETRYABLE".equals(root.path("upload").path("status").asText()),
                "network errors must be retryable");
        require(root.path("upload").path("attempts").asInt() == 1, "attempt count must persist");
        require(root.path("upload").hasNonNull("nextAttemptAt"), "retryable failure must record next attempt time");
    }

    private static Path writeCase(Path dir) throws Exception {
        Path path = dir.resolve("case.case.json");
        Files.writeString(path, """
                {
                  "schemaVersion": "1",
                  "caseId": "case-001",
                  "createdAt": "2026-06-27T01:02:03Z",
                  "caseType": "timeout",
                  "severity": "ERROR",
                  "licenseId": "license-001",
                  "app": {
                    "appId": "dhxy",
                    "version": "0.0.test",
                    "licenseId": "license-001"
                  },
                  "task": {
                    "taskCode": "xiuluo_v2"
                  },
                  "runtimeSnapshots": {
                    "environment": {
                      "machineHash": "machine-a"
                    }
                  },
                  "upload": {
                    "eligible": true,
                    "status": "PENDING",
                    "attempts": 0
                  }
                }
                """, StandardCharsets.UTF_8);
        return path;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;
        private final int port;

        private TestServer(HttpServer server) {
            this.server = server;
            this.port = server.getAddress().getPort();
        }

        private static TestServer start(int status, String body, AtomicInteger posts) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/case/upload", exchange -> handle(exchange, status, body, posts));
            server.start();
            return new TestServer(server);
        }

        private URI uri(String path) {
            return URI.create("http://127.0.0.1:" + port + path);
        }

        private static void handle(HttpExchange exchange, int status, String body, AtomicInteger posts) throws IOException {
            posts.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
