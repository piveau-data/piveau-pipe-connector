package io.piveau.pipe

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PipeLogger(pipeHeader: PipeHeader, segmentHeader: SegmentHeader) {
    private val log: Logger = LoggerFactory.getLogger("pipe.${pipeHeader.id}")
    private val prefix: String = "${pipeHeader.runId} [${pipeHeader.title} (${pipeHeader.context})] [${segmentHeader.title} (${segmentHeader.name})] "

    fun error(message: String?) = log.error(prefix + message)

    fun error(format: String?, argument1: Any?) = log.error(prefix + format, argument1)

    fun error(format: String?, argument1: Any?, argument2: Any?) = log.error(prefix + format, argument1, argument2)

    fun error(format: String?, vararg arguments: Any?) = log.error(prefix + format, *arguments)

    fun error(format: String?, e: Throwable?) = log.error(prefix + format, e)

    fun warn(message: String?) = log.warn(prefix + message)

    fun warn(format: String, argument1: Any) = log.warn(prefix + format, argument1)

    fun warn(format: String, argument1: Any, argument2: Any) = log.warn(prefix + format, argument1, argument2)

    fun warn(format: String, vararg arguments: Any) = log.warn(prefix + format, *arguments)

    fun warn(format: String, e: Throwable) = log.warn(prefix + format, e)

    fun info(message: String) = log.info(prefix + message)

    fun info(format: String, argument1: Any) = log.info(prefix + format, argument1)

    fun info(format: String, argument1: Any, argument2: Any) = log.info(prefix + format, argument1, argument2)

    fun info(format: String, vararg arguments: Any) = log.info(prefix + format, *arguments)

    fun info(format: String, e: Throwable) = log.info(prefix + format, e)

    fun debug(message: String) = log.debug(prefix + message)

    fun debug(format: String, argument1: Any) = log.debug(prefix + format, argument1)

    fun debug(format: String, argument1: Any, argument2: Any) = log.debug(prefix + format, argument1, argument2)

    fun debug(format: String, vararg arguments: Any) = log.debug(prefix + format, *arguments)

    fun debug(format: String, e: Throwable) = log.debug(prefix + format, e)

    fun trace(message: String) = log.trace(prefix + message)

    fun trace(format: String, argument1: Any) = log.trace(prefix + format, argument1)

    fun trace(format: String, argument1: Any, argument2: Any) = log.trace(prefix + format, argument1, argument2)

    fun trace(format: String, vararg arguments: Any) = log.trace(prefix + format, *arguments)

    fun trace(format: String, e: Throwable) = log.trace(prefix + format, e)

}