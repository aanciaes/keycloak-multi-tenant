package test.ktortemplate.common.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.Charset
import java.util.Base64

object JsonSettings {
    val mapper by lazy {
        val mapper = jacksonObjectMapper()
        mapper.apply {
            configure(SerializationFeature.INDENT_OUTPUT, true)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            })
        }
        mapper
    }

    val minMapper by lazy {
        val mapper = jacksonObjectMapper()
        mapper.apply {
            configure(SerializationFeature.INDENT_OUTPUT, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        }
        mapper
    }
}

fun String.customEncodeToBase64String(): String {
    return Base64.getEncoder().encodeToString(this.toByteArray(Charset.defaultCharset()))
}

fun String.customDecodeBase64String(): String {
    return String(Base64.getDecoder().decode(this), Charset.defaultCharset())
}
