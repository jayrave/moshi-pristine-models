package com.jayrave.moshi.pristineModels

import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PristineModelsJsonAdapterFactoryTest {

    @Test
    fun withOnlyMappers() {
        class ExampleModel()
        class ExampleModelMapper : ThrowingMapper<ExampleModel>()

        val factory = PristineModelsJsonAdapterFactory.Builder()
                .add(ExampleModel::class.java, ExampleModelMapper())
                .build()

        // Use factory & make sure that the adapter built for the required model
        // is from the Mapper
        val moshi = Moshi.Builder().add(factory).build()
        assertJsonAdapterIsFromMapperFor(moshi, ExampleModel::class.java)
    }


    @Test
    fun withOnlyMapperCreators() {
        class ExampleModel()
        class ExampleModelMapper : ThrowingMapper<ExampleModel>()

        val factory = PristineModelsJsonAdapterFactory.Builder()
                .add(ExampleModel::class.java, { ExampleModelMapper() })
                .build()

        // Use factory & make sure that the adapter built for the required model
        // is from the Mapper
        val moshi = Moshi.Builder().add(factory).build()
        assertJsonAdapterIsFromMapperFor(moshi, ExampleModel::class.java)
    }


    @Test
    fun mapperHasPrecedenceOverMapperCreator() {
        class ExampleModel()
        class ExampleModelMapper : ThrowingMapper<ExampleModel>()

        var mapperCreatorInvoked = false
        val mapperCreator = {
            mapperCreatorInvoked = true
            ExampleModelMapper()
        }

        val factory = PristineModelsJsonAdapterFactory.Builder()
                .add(ExampleModel::class.java, mapperCreator)
                .add(ExampleModel::class.java, ExampleModelMapper())
                .build()

        // Use factory & make sure that the adapter built for the required model
        // is from the Mapper
        val moshi = Moshi.Builder().add(factory).build()
        assertThat(mapperCreatorInvoked).isFalse() // To make sure mapper creator wasn't involved
        assertJsonAdapterIsFromMapperFor(moshi, ExampleModel::class.java)
    }


    @Test
    fun withBothMappersAndMapperCreators() {
        class ExampleModel1()
        class ExampleModel2()
        class ExampleModel1Mapper : ThrowingMapper<ExampleModel1>()
        class ExampleModel2Mapper : ThrowingMapper<ExampleModel2>()

        val factory = PristineModelsJsonAdapterFactory.Builder()
                .add(ExampleModel1::class.java, ExampleModel1Mapper())
                .add(ExampleModel2::class.java, { ExampleModel2Mapper() })
                .build()

        // Use factory & make sure that the adapter built for the required model
        // is from the Mapper
        val moshi = Moshi.Builder().add(factory).build()
        assertJsonAdapterIsFromMapperFor(moshi, ExampleModel1::class.java)
        assertJsonAdapterIsFromMapperFor(moshi, ExampleModel2::class.java)
    }



    private abstract class ThrowingMapper<T : Any> : Mapper<T>() {
        override fun create(value: Value<T>): T {
            throw UnsupportedOperationException("not implemented")
        }
    }



    companion object {
        private val JSON_ADAPTER_FROM_MAPPER_TO_STRING_REGEX = Regex(
                "com\\.jayrave\\.moshi\\.pristineModels\\.Mapper\\\$JsonAdapterForMapper@\\w+\\.nullSafe\\(\\)"
        )

        // TODO - This isn't a foolproof way to test this!
        private fun assertJsonAdapterIsFromMapperFor(moshi: Moshi, clazz: Class<*>) {
            val regexMatches = JSON_ADAPTER_FROM_MAPPER_TO_STRING_REGEX.matches(
                    moshi.adapter(clazz).toString()
            )

            assertThat(regexMatches).isTrue()
        }
    }
}