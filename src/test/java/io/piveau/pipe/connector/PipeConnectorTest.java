package io.piveau.pipe.connector;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Testing the pipe connector")
@ExtendWith(VertxExtension.class)
public class PipeConnectorTest {

    static PipeConnector connector;

    @BeforeAll
    @DisplayName("Initialize connector")
    public static void createConnector(Vertx vertx, VertxTestContext testContext) {

        PipeConnector.create(vertx, ar -> {
            if (ar.succeeded()) {
                connector = ar.result();
                testContext.completeNow();
            } else {
                testContext.failNow(ar.cause());
            }
        });

    }

    @Test
    @DisplayName("Testing handler")
    public void testHandler(Vertx vertx, VertxTestContext testContext) {
        Checkpoint checkpoint = testContext.checkpoint();

        connector.handler(pipeContext -> {
            System.out.println(pipeContext.getPipe().getHeader().getTitle());
            checkpoint.flag();
        });

        webInjection(vertx, testContext, "test-pipe.json");
    }

    @Test
    @DisplayName("Testing producer")
    public void testProducer(Vertx vertx, VertxTestContext testContext) {
        Checkpoint checkpoint = testContext.checkpoint();
        vertx.eventBus().consumer("io.piveau.pipe.connector.testing", msg -> {
            PipeContext pipeContext = (PipeContext)msg.body();
            System.out.println(pipeContext.getPipe().getHeader().getTitle());
            checkpoint.flag();
            msg.reply("success");
        });

        connector.consumerAddress("io.piveau.pipe.connector.testing");

        webInjection(vertx, testContext, "test-pipe.json");
    }

    private void webInjection(Vertx vertx, VertxTestContext testContext, String fileName) {
        Buffer buffer = vertx.fileSystem().readFileBlocking(fileName);
        JsonObject pipe = new JsonObject(buffer);
        WebClient client = WebClient.create(vertx);
        client.post(8080, "localhost", "/pipe")
                .putHeader("content-type", "application/json")
                .sendJsonObject(pipe, response -> {
                    if (response.succeeded()) {
                        if (response.result().statusCode() == 202) {
                            testContext.completeNow();
                        } else {
                            testContext.failNow(new Throwable(response.result().statusMessage()));
                        }
                    } else {
                        testContext.failNow(response.cause());
                    }
                });
    }

}