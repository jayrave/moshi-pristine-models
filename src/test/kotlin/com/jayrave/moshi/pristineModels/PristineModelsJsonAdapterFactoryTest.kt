package com.jayrave.moshi.pristineModels

import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.*
import org.junit.Test

class PristineModelsJsonAdapterFactoryTest {

    @Test
    fun pristineModelsJsonAdapterFactoryWorks() {
        class ExampleModel(val int: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int)
            }
        }

        val factory = PristineModelsJsonAdapterFactory.Builder()
                .add(ExampleModel::class.java, ExampleModelMapper())
                .build()

        // Use factory & make sure that the adapter built for the required model
        // is from the Mapper
        val moshi = Moshi.Builder().add(factory).build()
        assertThat(moshi.adapter(ExampleModel::class.java).javaClass.canonicalName).isEqualTo(
                "com.jayrave.moshi.pristineModels.Mapper.JsonAdapterForMapper"
        )
    }
}