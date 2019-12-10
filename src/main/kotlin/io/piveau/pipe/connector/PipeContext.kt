package io.piveau.pipe.connector

import com.fasterxml.jackson.databind.node.ObjectNode
import io.piveau.pipe.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import java.net.URL

class PipeContext private constructor(pipe: Pipe, private val pipeConnector: PipeConnector? = null) {

    val pipeManager = PipeManager.create(pipe)

    private val pipeLogger = PipeLogger(pipe.header, pipeManager.currentSegment!!.header)

    constructor(
        jsonObject: JsonObject,
        pipeConnector: PipeConnector? = null
    ) : this(Json.mapper.convertValue(jsonObject, Pipe::class.java), pipeConnector)

    constructor(buffer: Buffer, pipeConnector: PipeConnector? = null) : this(
        Json.decodeValue<Pipe>(
            buffer,
            Pipe::class.java
        ), pipeConnector
    )

    var forwarded: Boolean = false

    fun isForwarded(): Boolean = forwarded

    private var failureThrowable: Throwable? = null

    val failure: Boolean
        get() = failureThrowable != null

    fun isFailure(): Boolean = failure

    val cause: Throwable?
        get() = failureThrowable

    private var textResult: String? = null
    private var byteResult: ByteArray? = null

    private var mimeTypeResult: String? = null

    private var dataInfoResult: ObjectNode? = null

    val config = pipeManager.config

    val mimeType: String?
        get() = pipeManager.mimeType

    val dataInfo: ObjectNode
        get() = pipeManager.dataInfo

    val pipe: Pipe
        get() = pipeManager.pipe

    val log: PipeLogger
        get() = pipeLogger

    fun log() = pipeLogger

    val nextEndpoint: Endpoint?
        get() = pipeManager.nextEndpoint

    val stringData: String
        get() = pipeManager.data

    val binaryData: ByteArray
        get() = pipeManager.binaryData

    val message: JsonObject
        get() {
            val message = JsonObject().put("config", pipeManager.config)
            if (pipeManager.isBase64Payload) {
                message.put("data", pipeManager.binaryData).put("dataType", DataType.base64)
            } else {
                message.put("data", pipeManager.data).put("dataType", DataType.text)
            }
            return message
        }

    fun setResult(result: String, dataMimeType: String? = null, info: ObjectNode? = null): PipeContext {
        textResult = result
        mimeTypeResult = dataMimeType
        dataInfoResult = info
        forwarded = false
        return this
    }

    fun setResult(result: ByteArray, dataMimeType: String? = null, info: ObjectNode? = null): PipeContext {
        byteResult = result
        mimeTypeResult = dataMimeType
        dataInfoResult = info
        forwarded = false
        return this
    }

    fun getFinalPipe(): Buffer? {
        textResult?.let {
            pipeManager.setPayloadData(it, DataType.text, mimeTypeResult, dataInfoResult)
        } ?: byteResult?.let {
            pipeManager.setPayloadData(it, mimeTypeResult, dataInfoResult)
        }
        return Json.encodeToBuffer(pipeManager.getProcessedPipe())
    }

    fun forward() = pipeConnector?.forward(this)

    fun forward(vertx: Vertx) = forward(WebClient.create(vertx))

    fun forward(webClient: WebClient) {
        if (forwarded) {
            log.warn("Already forwarded")
            return
        }

        nextEndpoint?.let { endpoint ->
            getFinalPipe().let { pipe ->
                val address = URL(endpoint.address)
                val method = HttpMethod.valueOf(endpoint.method ?: "POST")
                webClient.request(method, address.port, address.host, address.path)
                    .putHeader("Content-Type", "application/json")
                    .sendBuffer(pipe) {
                        if (it.succeeded()) {
                            if (it.result().statusCode() == 200 || it.result().statusCode() == 202) {
                                log.trace("successfully forwarded")
                            } else {
                                log.error("{} - {}", it.result().statusCode(), it.result().statusMessage())
                            }
                        } else {
                            log.error("Forward request", it.cause())
                        }
                    }
            }
        } ?: log.trace("No next pipe segment available")

        forwarded = true
    }

    fun pass() = pipeConnector?.pass(this)

    fun pass(vertx: Vertx) = pass(WebClient.create(vertx))

    fun pass(webClient: WebClient) {
        if (forwarded) {
            log.warn("Already forwarded")
            return
        }

        if (pipeManager.isBase64Payload) {
            setResult(pipeManager.binaryData, pipeManager.mimeType, pipeManager.dataInfo)
        } else {
            setResult(pipeManager.data, pipeManager.mimeType, pipeManager.dataInfo)
        }
        forward(webClient)
    }

    fun setFailure(message: String) = setFailure(Throwable(message))

    fun setFailure(throwable: Throwable): PipeContext {
        failureThrowable = throwable

        log.error("Pipe failure: " + throwable.message, throwable.cause ?: throwable)

        if (pipeConnector?.isMailerEnabled == true) {
            pipeConnector.useMailer().send(this)
        }
        return this
    }

}
