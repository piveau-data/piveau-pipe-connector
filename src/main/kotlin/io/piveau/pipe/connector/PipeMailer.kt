package io.piveau.pipe.connector

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.MailAttachment
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.MailMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

class PipeMailer(vertx: Vertx, val config: JsonObject) {

    companion object {
        @JvmStatic
        fun create(vertx: Vertx, config: JsonObject): PipeMailer = PipeMailer(vertx, config)
    }

    private val log: Logger? = LoggerFactory.getLogger(this.javaClass)

    private val client: MailClient?

    private val logo: Buffer

    private val templateEngine: TemplateEngine

    init {
        val mailConfig = MailConfig(config.getString("host"))
        mailConfig.isTrustAll = true

        client = MailClient.createShared(vertx, mailConfig)

        logo = vertx.fileSystem().readFileBlocking("emails/piveau_logo.png")

        val templateResolver = ClassLoaderTemplateResolver().also {
            it.templateMode = TemplateMode.HTML
            it.prefix = "/emails/"
            it.suffix = ".html"
        }

        templateEngine = TemplateEngine()
        templateEngine.setTemplateResolver(templateResolver)
    }

    fun send(pipeContext: PipeContext) {
        val pipe = pipeContext.pipeManager.pipe
        val segment = pipeContext.pipeManager.currentSegment
        val address = pipeContext.pipeManager.config.path("mailto")?.asText(config.getString("mailto", "piveau-support@fokus.fraunhofer.de"))

        val attachment = MailAttachment()
        with(attachment) {
            contentType = "image/png"
            data = logo
            disposition = "inline"
            contentId = "logo"
        }

        val ctx = Context()
        ctx.setVariables(mapOf(
                "logo" to "logo",
                "header" to segment?.header?.title,
                "harvester" to pipe.header.title,
                "message" to pipeContext.cause?.message,
                "details" to JsonObject(segment?.body?.config.toString()).encodePrettily(),
                "run" to pipe.header.startTime
        ))

        val message = MailMessage()
        with(message) {
            to = listOf(address)
            from = "noreply@piveau.io"
            subject = "[${pipe.header.context} - ${config.getString("system", "consus")}] [${segment?.header?.name}] piveau event"
            inlineAttachment = listOf(attachment)
            html = templateEngine.process("simple", ctx)
        }

        client?.sendMail(message) { ar ->
            if (ar.succeeded()) {
                log?.debug("Mail sent: {}", ar.result().messageID)!!
            } else {
                log?.error("Mail sent", ar.cause())!!
            }
        }!!
    }

}
