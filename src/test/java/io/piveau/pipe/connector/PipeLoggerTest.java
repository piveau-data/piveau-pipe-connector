package io.piveau.pipe.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.piveau.pipe.Pipe;
import io.piveau.pipe.PipeLogger;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

@DisplayName("Testing the pipe connector")
@ExtendWith(VertxExtension.class)
public class PipeLoggerTest {

    @Test
    @DisplayName("Testing handler")
    public void logTest(Vertx vertx, VertxTestContext testContext) {
        vertx.fileSystem().readFile("test-pipe.json", result -> {
            if (result.succeeded()) {
                try {
                    Pipe pipe = new ObjectMapper().readValue(result.result().toString(), Pipe.class);
                    PipeLogger logger = new PipeLogger(pipe.getHeader(), pipe.getBody().getSegments().get(0).getHeader());
                    logger.info("Test");
                    logger.info("Test {}", "param1");
                    logger.info("Test {}, {}", "param1", "param2");
                    logger.info("Test {}, {}, {}", "param1", "param2", "param3");
                    logger.info("Test", new Throwable("throwable1"));
                    testContext.completeNow();
                } catch (IOException e) {
                    testContext.failNow(e);
                }
            }
        });
    }

}
