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
import java.util.concurrent.CountDownLatch

class FieldMappingImplTest {

    @Test
    fun valueIsRead() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val fieldValue = 5
        val fieldBinding = FieldMappingImpl(fieldName, ExampleModel::int)

        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.read(jsonReaderFrom(jsonString(fieldValue)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isEqualTo(fieldValue)
    }


    @Test
    fun valueIsWritten() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val model = ExampleModel(5)
        val fieldBinding = FieldMappingImpl(fieldName, ExampleModel::int)

        val stringSink = StringSink.create()
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.write(jsonWriterTo(stringSink), model)
        assertThat(stringSink.toString()).isEqualTo(jsonString(model.int))
    }


    @Test(expected = IllegalStateException::class)
    fun readWithoutAcquiringAdapterThrowsWhenNotUsingExplicitJsonAdapter() {
        class ExampleModel(val int: Int)
        FieldMappingImpl("int_field", ExampleModel::int).read(jsonReaderFrom(jsonString(5)))
    }


    @Test(expected = IllegalStateException::class)
    fun writeWithoutAcquiringAdapterThrowsWhenNotUsingExplicitJsonAdapter() {
        class ExampleModel(val int: Int)
        FieldMappingImpl("int_field", ExampleModel::int).write(
                jsonWriterTo(StringSink.create()), ExampleModel(5)
        )
    }


    @Test
    fun explicitlyPassedInJsonAdapterIsUsed() {
        val trueInt = BooleanAsIntJsonAdapter.TRUE_INT
        val falseInt = BooleanAsIntJsonAdapter.FALSE_INT
        class ExampleModel(val boolean: Boolean)

        val fieldBinding = FieldMappingImpl("b", ExampleModel::boolean, BooleanAsIntJsonAdapter())
        fieldBinding.read(jsonReaderFrom(jsonString(trueInt)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isTrue()
        fieldBinding.read(jsonReaderFrom(jsonString(falseInt)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isFalse()

        val stringSink1 = StringSink.create()
        fieldBinding.write(jsonWriterTo(stringSink1), ExampleModel(true))
        assertThat(stringSink1.toString()).isEqualTo(jsonString(trueInt))

        val stringSink2 = StringSink.create()
        fieldBinding.write(jsonWriterTo(stringSink2), ExampleModel(false))
        assertThat(stringSink2.toString()).isEqualTo(jsonString(falseInt))
    }


    @Test
    fun explicitlyPassedInJsonAdapterIsNotOverwrittenEvenIfAcquiredAdapterIsCalled() {
        val trueInt = BooleanAsIntJsonAdapter.TRUE_INT
        class ExampleModel(val boolean: Boolean)

        // Build binding with custom adapter & ask to acquire json adapter
        val fieldBinding = FieldMappingImpl("b", ExampleModel::boolean, BooleanAsIntJsonAdapter())
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())

        fieldBinding.read(jsonReaderFrom(jsonString(trueInt)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isTrue()

        val stringSink = StringSink.create()
        fieldBinding.write(jsonWriterTo(stringSink), ExampleModel(true))
        assertThat(stringSink.toString()).isEqualTo(jsonString(trueInt))
    }


    @Test
    fun readWorksInMultiThreadedScenarios() {
        class ExampleModel(val int: Int)

        val fieldName = "int_field"
        val fieldBinding = FieldMappingImpl(fieldName, ExampleModel::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())

        fun runThread(
                threadSpecificFieldValue: Int, latch1: CountDownLatch,
                latch2: CountDownLatch, awaitOnLatch: CountDownLatch) {

            Thread {
                // Read & assert value
                fieldBinding.read(jsonReaderFrom(jsonString(threadSpecificFieldValue)))
                assertThat(fieldBinding.lastReadValueInCurrentThread()).isEqualTo(
                        threadSpecificFieldValue
                )

                // Count down & await on the other latch
                latch1.countDown()
                awaitOnLatch.await()

                // Assert again
                assertThat(fieldBinding.lastReadValueInCurrentThread()).isEqualTo(
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

        val fieldBinding = FieldMappingImpl("nullable_int_field", ExampleModelWithNullableType::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())

        // Assert exception is not thrown when value is null
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isNull()

        // Assert exception is not thrown when value is non-null
        fieldBinding.read(jsonReaderFrom(jsonString(5)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isEqualTo(5)
    }


    @Test(expected = FieldMappingImpl.ValueCanNotBeNullException::class)
    fun valueCanNotBeNullExceptionIsThrownForNonNullTypeIfValueIsNull() {
        class ExampleModel(val int: Int)

        val fieldBinding = FieldMappingImpl("int_field", ExampleModel::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.lastReadValueInCurrentThread()
    }


    @Test
    fun clearLastReadValueInCurrentThreadWorks() {
        class ExampleModelWithNullableType(val int: Int?)

        val fieldBinding = FieldMappingImpl("nullable_int_field", ExampleModelWithNullableType::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())

        // Read & assert value was read
        fieldBinding.read(jsonReaderFrom(jsonString(5)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isNotNull()

        // Clear & assert value was cleared
        fieldBinding.clearLastReadValueInCurrentThread()
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isNull()
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
}