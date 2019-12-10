package io.piveau.pipe.connector;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

class PipeContextMessageCodec implements MessageCodec<JsonObject, PipeContext> {

    private PipeConnector pipeConnector;

    PipeContextMessageCodec(PipeConnector pipeConnector) {
        this.pipeConnector = pipeConnector;
    }

    @Override
    public void encodeToWire(Buffer buffer, JsonObject entries) {

    }

    @Override
    public PipeContext decodeFromWire(int i, Buffer buffer) {
        return null;
    }

    @Override
    public PipeContext transform(JsonObject entries) {
        return new PipeContext(entries, pipeConnector);
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }

}
