package com.felipejaner.quotes;

import com.felipejaner.quotes.submission.HttpInsurerGateway;
import com.felipejaner.quotes.error.InsurerUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class HttpInsurerGatewayTest {
    HttpServer server;
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/success", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        server.createContext("/failure", ex -> { ex.sendResponseHeaders(503, -1); ex.close(); });
        server.createContext("/slow", ex -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ex.sendResponseHeaders(200, -1); ex.close();
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    HttpInsurerGateway gateway(String path, Duration timeout) {
        return new HttpInsurerGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path), timeout);
    }
    @Test void acceptsRealHttpSuccess() { assertThatCode(() -> gateway("/success", Duration.ofSeconds(1)).submit(UUID.randomUUID())).doesNotThrowAnyException(); }
    @Test void translatesHttpFailureAndTimeout() {
        assertThatThrownBy(() -> gateway("/failure", Duration.ofSeconds(1)).submit(UUID.randomUUID())).isInstanceOf(InsurerUnavailableException.class).hasMessageContaining("503");
        assertThatThrownBy(() -> gateway("/slow", Duration.ofMillis(50)).submit(UUID.randomUUID())).isInstanceOf(InsurerUnavailableException.class).hasMessageContaining("retry");
    }
}
