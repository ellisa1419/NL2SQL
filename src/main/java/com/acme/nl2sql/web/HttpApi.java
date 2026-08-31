package com.acme.nl2sql.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The HTTP surface, on the JDK's built-in server.
 *
 * <pre>
 *   POST /v1/query          answer a question
 *   GET  /v1/metrics        timings, SLA breaches, error rate
 *   POST /v1/metrics/reset  clear collected metrics
 *   GET  /v1/schema         the table definitions
 * </pre>
 */
public class HttpApi {

    private static final Logger log = LoggerFactory.getLogger(HttpApi.class);

    private static final String JSON = "application/json;charset=UTF-8";
    private static final String TEXT = "text/plain;charset=UTF-8";
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() { };

    private final QueryController controller;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService workers;

    /**
     * Binds the listen socket and registers the routes. Does not start serving.
     *
     * @param controller the endpoint implementations
     * @param host       interface to bind
     * @param port       port to bind
     * @throws IllegalStateException if the socket cannot be bound
     */
    public HttpApi(QueryController controller, String host, int port) {
        this.controller = controller;
        this.workers = Executors.newCachedThreadPool();
        try {
            this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        } catch (IOException e) {
            throw new IllegalStateException("could not bind " + host + ":" + port, e);
        }
        server.setExecutor(workers);
        server.createContext("/v1/query", ex -> handle(ex, "POST", this::query));
        server.createContext("/v1/metrics", ex -> handle(ex, "GET", x -> writeJson(x, 200, controller.metrics())));
        server.createContext("/v1/metrics/reset", ex -> handle(ex, "POST", x -> writeJson(x, 200, controller.reset())));
        server.createContext("/v1/schema", ex -> handle(ex, "GET", x -> write(x, 200, TEXT, controller.schema())));
    }

    /** Starts serving requests on a background thread pool. */
    public void start() {
        server.start();
        log.info("listening on port {}", server.getAddress().getPort());
    }

    /**
     * Stops accepting requests and drains in-flight work.
     *
     * @param graceSeconds how long to let running exchanges finish
     */
    public void stop(int graceSeconds) {
        server.stop(graceSeconds);
        workers.shutdown();
        try {
            if (!workers.awaitTermination(graceSeconds, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Applies method checking and blanket error handling around a route.
     *
     * @param ex     the exchange
     * @param method the only method this route accepts
     * @param route  the route body
     * @throws IOException if the response cannot be written
     */
    private void handle(HttpExchange ex, String method, Route route) throws IOException {
        try {
            if (!ex.getRequestURI().getPath().equals(ex.getHttpContext().getPath())) {
                writeJson(ex, 404, Map.of("error", "no handler for " + ex.getRequestURI().getPath()));
            } else if (!method.equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Allow", method);
                writeJson(ex, 405, Map.of("error", ex.getRequestMethod() + " not allowed"));
            } else {
                route.accept(ex);
            }
        } catch (BadRequestException e) {
            writeJson(ex, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("request failed: {} {}", ex.getRequestMethod(), ex.getRequestURI(), e);
            writeJson(ex, 500, Map.of("error", String.valueOf(e.getMessage())));
        } finally {
            ex.close();
        }
    }

    /**
     * Decodes the request body and runs a query.
     *
     * @param ex the exchange
     * @throws IOException if reading or writing fails
     */
    private void query(HttpExchange ex) throws IOException {
        Map<String, String> body;
        try (InputStream in = ex.getRequestBody()) {
            body = json.readValue(in.readAllBytes(), STRING_MAP);
        } catch (Exception e) {
            throw new BadRequestException("malformed JSON body: " + e.getMessage());
        }
        if (body == null || body.get("question") == null || body.get("question").isBlank()) {
            throw new BadRequestException("question is required");
        }
        writeJson(ex, 200, controller.query(body));
    }

    private void writeJson(HttpExchange ex, int status, Object payload) throws IOException {
        write(ex, status, JSON, json.writeValueAsString(payload));
    }

    private void write(HttpExchange ex, int status, String contentType, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** A route body. */
    private interface Route {
        void accept(HttpExchange ex) throws IOException;
    }

    /** Signals a client-side problem that should surface as 400. */
    private static class BadRequestException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        BadRequestException(String message) {
            super(message);
        }
    }
}
