package com.jayrave.moshi.pristineModels

import com.squareup.moshi.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

internal class FieldMappingImpl<T : Any, F>(
        override val name: String, private val property: KProperty1<T, F>) :
        FieldMapping<T, F> {

    private var jsonAdapter: JsonAdapter<F>? = null
    private val readValues = ThreadLocal<F?>()
    private val valueCanBeNull = property.returnType.isMarkedNullable

    fun acquireJsonAdapter(moshi: Moshi) {
        jsonAdapter = moshi.adapter(property.returnType.javaType)
    }

    @Throws(ValueCanNotBeNullException::class)
    fun lastReadValueInCurrentThread(): F {
        // It seems that even if [F] is a non-null type, this method will happily return a
        // `null` value (they are no nullability checks I could see in the decompiled code.
        // I guess because of erasure there isn't enough information). Therefore do a manual
        // check & throw if required

        val value = readValues.get() as F
        return when (valueCanBeNull) {
            true -> value
            else -> when (value) {
                null -> throw ValueCanNotBeNullException("$name can't be null")
                else -> value
            }
        }
    }

    fun clearLastReadValueInCurrentThread() = readValues.remove()
    fun read(reader: JsonReader) = readValues.set(acquiredAdapter().fromJson(reader))
    fun write(writer: JsonWriter, model: T) = acquiredAdapter().toJson(writer, property.get(model))
    private fun acquiredAdapter(): JsonAdapter<F> {
        return jsonAdapter ?: throw IllegalStateException(
                "Adapter is still not acquired for property: $name"
        )
    }


    /**
     * Thrown when `null` seems to be the value for a non-null type
     */
    internal class ValueCanNotBeNullException(message: String) : RuntimeException(message)
}