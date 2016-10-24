package com.jayrave.moshi.pristineModels.testLib

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonPrimitive
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer
import okio.BufferedSink

val DEFAULT_CHARSET = Charsets.UTF_8

fun jsonString(vararg values: Pair<String, *>) = jsonObject(*values).toString()
fun jsonString(number: Number) = JsonPrimitive(number).toString()

fun jsonWriterTo(sink: BufferedSink, serializeNulls: Boolean = true): JsonWriter {
    val writer = JsonWriter.of(sink)
    if (serializeNulls) {
        writer.serializeNulls = true
    }

    return writer
}

fun jsonReaderFrom(jsonString: String): JsonReader = JsonReader.of(
        Buffer().writeString(jsonString, DEFAULT_CHARSET)
)

class StringSink private constructor(private val buffer: Buffer) : BufferedSink by buffer {
    override fun toString(): String = buffer.readString(DEFAULT_CHARSET)
    companion object {
        fun create() = StringSink(Buffer())
    }
}