package com.jayrave.moshi.pristineModels

import com.squareup.moshi.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

internal class FieldMappingImpl<T, F>(
        override val name: String, private val property: KProperty1<T, F>) :
        FieldMapping {

    private var jsonAdapter: JsonAdapter<F>? = null
    private val readValues = ThreadLocal<F?>()

    fun acquireJsonAdapter(moshi: Moshi) {
        jsonAdapter = moshi.adapter(property.returnType.javaType)
    }

    /**
     * It seems that even if [F] is a non-null type, [lastReadValue] can happily return a
     * `null` value & only when this value is assigned (or done something else with) by
     * the calling code, a null pointer exception (from java.lang) is thrown
     */
    fun lastReadValue(): F = readValues.get() as F

    fun read(reader: JsonReader) = readValues.set(acquiredAdapter().fromJson(reader))
    fun write(writer: JsonWriter, value: F) = acquiredAdapter().toJson(writer, value)
    private fun acquiredAdapter(): JsonAdapter<F> {
        return jsonAdapter ?: throw IllegalStateException(
                "Adapter is still not acquired for property: $name"
        )
    }
}