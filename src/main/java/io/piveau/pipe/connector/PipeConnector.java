package io.piveau.pipe.connector;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipeConnector extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Handler<PipeContext> handler;

    private String consumer;

    private PipeMailer pipeMailer;

    private Router router;

    public static PipeConnector create() {
        return create(new DeploymentOptions());
    }

    public static PipeConnector create(DeploymentOptions deploymentOptions) {
        Vertx vertx = Vertx.vertx();

        PipeConnector pipeConnector = new PipeConnector();
        vertx.deployVerticle(pipeConnector, deploymentOptions);
        vertx.eventBus().registerCodec(new PipeContextMessageCodec(pipeConnector));
        return pipeConnector;
    }

    public static void create(Vertx vertx, Handler<AsyncResult<PipeConnector>> handler) {
        create(vertx, new DeploymentOptions(), handler);
    }

    public static void create(Vertx vertx, DeploymentOptions deploymentOptions, Handler<AsyncResult<PipeConnector>> handler) {
        PipeConnector pipeConnector = new PipeConnector();
        vertx.eventBus().registerCodec(new PipeContextMessageCodec(pipeConnector));
        vertx.deployVerticle(pipeConnector, deploymentOptions, ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(pipeConnector));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        MetricsService metrics = MetricsService.create(vertx);

        ConfigStoreOptions storeOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray()
                        .add("PIVEAU_PIPE_ENDPOINT_PORT")
                        .add("PIVEAU_PIPE_MAIL_CONFIG")));

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(storeOptions));
        retriever.getConfig(lc -> {
            if (lc.succeeded()) {
                JsonObject config = lc.result();
                if (config.containsKey("PIVEAU_PIPE_MAIL_CONFIG")) {
                    JsonObject mailConfig = config.getJsonObject("PIVEAU_PIPE_MAIL_CONFIG");
                    pipeMailer = PipeMailer.create(vertx, mailConfig);
                }
                OpenAPI3RouterFactory.create(vertx, "webroot/openapi-pipe.yaml", ar -> {
                    if (ar.succeeded()) {

                        OpenAPI3RouterFactory routerFactory = ar.result();
                        RouterFactoryOptions options = new RouterFactoryOptions().setMountNotImplementedHandler(true);
                        routerFactory.setOptions(options);
                        routerFactory.addHandlerByOperationId("incomingPipe", this::incomingPipeHandler);
                        routerFactory.addHandlerByOperationId("configSchema", this::configSchemaHandler);

                        router = routerFactory.getRouter();
                        router.route().order(0).handler(CorsHandler.create("*").allowedMethods(Stream.of(HttpMethod.POST).collect(Collectors.toSet())));

                        router.route("/*").handler(StaticHandler.create());

                        HealthCheckHandler hch = HealthCheckHandler.create(vertx);
                        hch.register("buildInfo", future -> vertx.fileSystem().readFile("buildInfo.json", bi -> {
                            if (bi.succeeded()) {
                                future.complete(Status.OK(bi.result().toJsonObject()));
                            } else {
                                future.fail(bi.cause());
                            }
                        }));
                        router.get("/health").handler(hch);

                        router.get("/metrics").produces("application/json").handler(routingContext -> {
                            JsonObject m = metrics.getMetricsSnapshot(vertx);
                            if (m != null) {
                                routingContext.response().setStatusCode(200).end(m.encodePrettily());
                            } else {
                                routingContext.response().setStatusCode(503).end();
                            }
                        });

                        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(config.getInteger("PIVEAU_PIPE_ENDPOINT_PORT", 8080)));
                        server.requestHandler(router).listen();

                        startPromise.complete();
                    } else {
                        startPromise.fail(ar.cause());
                    }
                });
            }
        });
    }

    public void handler(Handler<PipeContext> handler) {
        this.handler = handler;
        this.consumer = null;
    }

    public void consumerAddress(String consumer) {
        this.consumer = consumer;
        this.handler = null;
    }

    public void subRouter(String mountPoint, Router router) {
        if (router != null) {
            this.router.mountSubRouter(mountPoint, router);
        } else {
            throw new IllegalStateException("Router not set.");
        }
    }

    public void incomingPipeHandler(RoutingContext routingContext) {
        JsonObject pipe = routingContext.getBodyAsJson();
        if (!pipe.getJsonObject("header").containsKey("runId")) {
            pipe.getJsonObject("header").put("runId", UUID.randomUUID().toString());
        }
        if (!pipe.getJsonObject("header").containsKey("startTime")) {
            pipe.getJsonObject("header").put("startTime", new Date().toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (consumer != null) {
            vertx.eventBus().send(consumer, pipe, new DeliveryOptions().setCodecName(new PipeContextMessageCodec(this).name()));
            routingContext.response().setStatusCode(202).end(pipe.getJsonObject("header").getString("runId"));
        } else if (handler != null) {
            PipeContext pipeContext = new PipeContext(pipe, this);

            vertx.executeBlocking(future -> {
                handler.handle(pipeContext);
                if (pipeContext.isFailure()) {
                    future.fail(pipeContext.getCause());
                } else {
                    if (!pipeContext.isForwarded()) {
                        pipeContext.forward(vertx);
                    }
                    future.complete();
                }
            }, result -> {
                if (result.succeeded()) {
                    pipeContext.log().trace("Incoming pipe successfully handled by handler.");
                } else {
                    pipeContext.log().error("Handling pipe", result.cause());
                }
            });
            routingContext.response().setStatusCode(202).end(pipe.getJsonObject("header").getString("runId"));
        } else {
            routingContext.response().setStatusCode(501).end();
        }

    }

    public void configSchemaHandler(RoutingContext routingContext) {
        vertx.fileSystem().readFile("config.schema.json", ar -> {
            if (ar.succeeded()) {
                routingContext.response().putHeader("Content-Type", "application/schema+json").end(ar.result());
            } else {
                routingContext.response().setStatusCode(404).end();
            }
        });
    }

    public void forward(PipeContext pipeContext) {
        if (!pipeContext.isForwarded()) {
            pipeContext.forward(vertx);
        }
    }

    public void pass(PipeContext pipeContext) {
        if (!pipeContext.isForwarded()) {
            pipeContext.pass(vertx);
        }
    }

    public boolean isMailerEnabled() {
        return pipeMailer != null;
    }

    public PipeMailer useMailer() {
        return pipeMailer;
    }

}
