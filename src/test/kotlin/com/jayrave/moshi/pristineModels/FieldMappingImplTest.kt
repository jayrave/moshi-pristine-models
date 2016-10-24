package com.jayrave.moshi.pristineModels

import com.jayrave.moshi.pristineModels.testLib.StringSink
import com.jayrave.moshi.pristineModels.testLib.jsonReaderFrom
import com.jayrave.moshi.pristineModels.testLib.jsonString
import com.jayrave.moshi.pristineModels.testLib.jsonWriterTo
import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch

class FieldMappingImplTest {

    @Test
    fun valueIsRead() {
        val fieldName = "int_field"
        val fieldValue = 5
        val fieldBinding = FieldMappingImpl(fieldName, ExampleModel::int)

        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.read(jsonReaderFrom(jsonString(fieldValue)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isEqualTo(fieldValue)
    }


    @Test
    fun valueIsWritten() {
        val fieldName = "int_field"
        val fieldValue = 5
        val fieldBinding = FieldMappingImpl(fieldName, ExampleModel::int)

        val stringSink = StringSink.create()
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.write(jsonWriterTo(stringSink), fieldValue)
        assertThat(stringSink.toString()).isEqualTo(jsonString(fieldValue))
    }


    @Test(expected = IllegalStateException::class)
    fun readWithoutAcquiringAdapterThrows() {
        FieldMappingImpl("int_field", ExampleModel::int).read(jsonReaderFrom(jsonString(5)))
    }


    @Test(expected = IllegalStateException::class)
    fun writeWithoutAcquiringAdapterThrows() {
        FieldMappingImpl("int_field", ExampleModel::int).write(jsonWriterTo(StringSink.create()), 5)
    }


    @Test
    fun readWorksInMultiThreadedScenarios() {
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
        val fieldBinding = FieldMappingImpl("int_field", ExampleModel::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())
        fieldBinding.lastReadValueInCurrentThread()
    }


    @Test
    fun clearLastReadValueInCurrentThreadWorks() {
        val fieldBinding = FieldMappingImpl("nullable_int_field", ExampleModelWithNullableType::int)
        fieldBinding.acquireJsonAdapter(Moshi.Builder().build())

        // Read & assert value was read
        fieldBinding.read(jsonReaderFrom(jsonString(5)))
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isNotNull()

        // Clear & assert value was cleared
        fieldBinding.clearLastReadValueInCurrentThread()
        assertThat(fieldBinding.lastReadValueInCurrentThread()).isNull()
    }



    private data class ExampleModel(val int: Int)
    private data class ExampleModelWithNullableType(val int: Int?)
}