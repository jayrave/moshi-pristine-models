package com.jayrave.moshi.pristineModels

import com.jayrave.moshi.pristineModels.testLib.StringSink
import com.jayrave.moshi.pristineModels.testLib.jsonReaderFrom
import com.jayrave.moshi.pristineModels.testLib.jsonString
import com.jayrave.moshi.pristineModels.testLib.jsonWriterTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType

class FieldMappingImplTest {

    @Test
    fun existingJsonAdapterOrAcquireFromReturnsExistingAdapter() {
        class ExampleModel(@Suppress("unused") val boolean: Boolean)
        val fieldMapping = buildFieldMapping("boolean_field", ExampleModel::boolean)

        // Make the mapping acquire json adapter
        fieldMapping.acquireJsonAdapterIfRequired(Moshi.Builder().add({ type, _, _ -> when (type) {
            Boolean::class.javaPrimitiveType -> BooleanAsIntJsonAdapter()
            else -> null
        } }).build())

        val actualAdapter = fieldMapping.existingJsonAdapterOrAcquireFrom(Moshi.Builder().build())
        assertThat(actualAdapter).isInstanceOf(BooleanAsIntJsonAdapter::class.java)
    }


    @Test
    fun existingJsonAdapterOrAcquireFromAcquiresInCaseAdapterDoesNotExistAlready() {
        class ExampleModel(@Suppress("unused") val boolean: Boolean)
        val fieldMapping = buildFieldMapping("boolean_field", ExampleModel::boolean)
        val moshi = Moshi.Builder().add({ type, _, _ -> when (type) {
            Boolean::class.javaPrimitiveType -> BooleanAsIntJsonAdapter()
            else -> null
        } }).build()

        val actualAdapter = fieldMapping.existingJsonAdapterOrAcquireFrom(moshi)
        assertThat(actualAdapter).isInstanceOf(BooleanAsIntJsonAdapter::class.java)
    }


    @Test
    fun valueIsRead() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val fieldValue = 5
        val fieldMapping = buildFieldMapping(fieldName, ExampleModel::int)

        fieldMapping.acquireJsonAdapterIfRequired(Moshi.Builder().build())
        val valueFromJson = fieldMapping.read(jsonReaderFrom(jsonString(fieldValue)))
        assertThat(valueFromJson).isEqualTo(fieldValue)
    }


    @Test
    fun valueIsWritten() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val model = ExampleModel(5)
        val fieldMapping = buildFieldMapping(fieldName, ExampleModel::int)

        val stringSink = StringSink.create()
        fieldMapping.acquireJsonAdapterIfRequired(Moshi.Builder().build())
        fieldMapping.write(jsonWriterTo(stringSink), model)
        assertThat(stringSink.toString()).isEqualTo(jsonString(model.int))
    }


    @Test(expected = IllegalStateException::class)
    fun readWithoutAcquiringAdapterThrowsWhenNotUsingExplicitJsonAdapter() {
        class ExampleModel(val int: Int)
        buildFieldMapping("int_field", ExampleModel::int).read(jsonReaderFrom(jsonString(5)))
    }


    @Test(expected = IllegalStateException::class)
    fun writeWithoutAcquiringAdapterThrowsWhenNotUsingExplicitJsonAdapter() {
        class ExampleModel(val int: Int)
        buildFieldMapping("int_field", ExampleModel::int).write(
                jsonWriterTo(StringSink.create()), ExampleModel(5)
        )
    }


    @Test
    fun explicitlyPassedInJsonAdapterIsUsed() {
        val trueInt = BooleanAsIntJsonAdapter.TRUE_INT
        val falseInt = BooleanAsIntJsonAdapter.FALSE_INT
        class ExampleModel(val boolean: Boolean)

        val fieldMapping = buildFieldMapping("b", ExampleModel::boolean, BooleanAsIntJsonAdapter())
        var valueFromJson = fieldMapping.read(jsonReaderFrom(jsonString(trueInt)))
        assertThat(valueFromJson).isTrue()
        valueFromJson = fieldMapping.read(jsonReaderFrom(jsonString(falseInt)))
        assertThat(valueFromJson).isFalse()

        val stringSink1 = StringSink.create()
        fieldMapping.write(jsonWriterTo(stringSink1), ExampleModel(true))
        assertThat(stringSink1.toString()).isEqualTo(jsonString(trueInt))

        val stringSink2 = StringSink.create()
        fieldMapping.write(jsonWriterTo(stringSink2), ExampleModel(false))
        assertThat(stringSink2.toString()).isEqualTo(jsonString(falseInt))
    }


    @Test
    fun explicitlyPassedInJsonAdapterIsNotOverwrittenEvenIfAcquiredAdapterIsCalled() {
        val trueInt = BooleanAsIntJsonAdapter.TRUE_INT
        class ExampleModel(val boolean: Boolean)

        // Build binding with custom adapter & ask to acquire json adapter
        val fieldMapping = buildFieldMapping("b", ExampleModel::boolean, BooleanAsIntJsonAdapter())
        fieldMapping.acquireJsonAdapterIfRequired(Moshi.Builder().build())

        val valueFromJson = fieldMapping.read(jsonReaderFrom(jsonString(trueInt)))
        assertThat(valueFromJson).isTrue()

        val stringSink = StringSink.create()
        fieldMapping.write(jsonWriterTo(stringSink), ExampleModel(true))
        assertThat(stringSink.toString()).isEqualTo(jsonString(trueInt))
    }


    @Test
    fun readWorksInMultiThreadedScenarios() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val fieldMapping = buildFieldMapping(fieldName, ExampleModel::int)
        fieldMapping.acquireJsonAdapterIfRequired(Moshi.Builder().build())

        fun runThread(
                threadSpecificFieldValue: Int, latch1: CountDownLatch,
                latch2: CountDownLatch, awaitOnLatch: CountDownLatch) {

            Thread {
                // Read & assert value
                val valueFromJson = fieldMapping.read(jsonReaderFrom(jsonString(threadSpecificFieldValue)))
                assertThat(valueFromJson).isEqualTo(
                        threadSpecificFieldValue
                )

                // Count down & await on the other latch
                latch1.countDown()
                awaitOnLatch.await()

                // Assert again
                assertThat(valueFromJson).isEqualTo(
                        threadSpecificFieldValue
                )

                // Count down the second latch
                latch2.countDown()
            }.start()
        }

        val thread1Latch1 = CountDownLatch(1)
        val thread1Latch2 = CountDownLatch(1)
        val thread2Latch1 = CountDownLatch(1)
        val thread2Latch2 = CountDownLatch(1)

        runThread(5, thread1Latch1, thread1Latch2, thread2Latch1)
        runThread(12, thread2Latch1, thread2Latch2, thread1Latch1)

        thread1Latch2.await()
        thread2Latch2.await()
    }


    @Test
    fun valueCanNotBeNullExceptionNotThrownForNullableType() {
        class ExampleModelWithNullableType(val int: Int?)

        val mapping = buildFieldMapping("nullable_int_field", ExampleModelWithNullableType::int)
        mapping.acquireJsonAdapterIfRequired(Moshi.Builder().build())

        // Assert exception is not thrown when value is non-null
        val valueFromJson = mapping.read(jsonReaderFrom(jsonString(5)))
        assertThat(valueFromJson).isEqualTo(5)
    }

    private class BooleanAsIntJsonAdapter : JsonAdapter<Boolean>() {
        override fun fromJson(reader: JsonReader): Boolean {
            return when (reader.nextInt()) {
                TRUE_INT -> true
                FALSE_INT -> false
                else -> throw IllegalArgumentException()
            }
        }

        override fun toJson(writer: JsonWriter, value: Boolean) {
            when (value) {
                true -> writer.value(TRUE_INT)
                else -> writer.value(FALSE_INT)
            }
        }


        companion object {
            const val TRUE_INT = 1
            const val FALSE_INT = 0
        }
    }



    companion object {
        private fun <T : Any, F> buildFieldMapping(
                name: String, property: KProperty1<T, F>, jsonAdapter: JsonAdapter<F>? = null):
                FieldMappingImpl<T, F> {

            val propertyExtractor = object : PropertyExtractor<T, F> {
                override val type: Type = property.returnType.javaType
                override fun extractFrom(t: T): F = property.get(t)
            }

            return FieldMappingImpl(
                    name, property.returnType.isMarkedNullable, propertyExtractor, jsonAdapter
            )
        }
    }
}