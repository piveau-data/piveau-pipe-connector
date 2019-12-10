# piveau pipe connector

## Features
 * Asynchronous event based pipe handling
 * piveau conformable logging
 * Internal management of pipe structure
 * Convenient API
 * Vert.x support

## Get started

The most simple example would be:
```java
PipeConnector.create().handle(pipeContext -> pipeContext.setResult("Hello World!"));
```

Of course, a more practical example would be this:
```java
PipeConnector connector = PipeConnector.create();
connector.handle(pipeContext -> {
    JsonNode config = pipeContext.getConfig();
    String data = pipeContext.getStringData();

    // do your job here
    String result = ...
    
    pipeContext.setResult(result); 
});
```

If you're already in a Vert.x context and would like to use a worker Verticle with an event bus address, this will do:
```java
PipeConnector.create(vertx, ar -> {
    if (ar.succeeded()) {
        ar.result().consumerAddress(EVENTBUS_ADDRESS);
        startFuture.complete();
    } else {
        startFuture.fail(ar.cause());
    }
});
```

## Service Interface

* `/pipe`

    Pipe endpoint (see ReDoc on root context path)

* `/health`

    Returns up status and build infos. Requires a buildInfo.json in the classpath, e.g.:
    ```json
    {
       "timestamp": "${buildTimestamp}",
       "version": "${project.version}"
    }
    ```
    `timestamp` and `version` could be set automatically by maven. 

* `/metrics`

    Internal metric infos

It is possible to mount a sub-router for your own interfaces:

```java
connector.subRouter("/mountPoint", router);
```

## Configuration

 * PIVEAU_PIPE_ENDPOINT_PORT - Default port is 8080
 * PIVEAU_PIPE_MAIL_CONFIG - JSON object:
   
   ```json
   {
      "host": "<smtp_host>",
      "system": "<environment>",
      "mailto": "<receiver_address>"
   }
   ```
  