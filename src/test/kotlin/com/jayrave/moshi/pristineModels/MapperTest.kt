package com.jayrave.moshi.pristineModels

import com.jayrave.moshi.pristineModels.testLib.StringSink
import com.jayrave.moshi.pristineModels.testLib.jsonReaderFrom
import com.jayrave.moshi.pristineModels.testLib.jsonString
import com.jayrave.moshi.pristineModels.testLib.jsonWriterTo
import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MapperTest {

    @Test
    fun canMapFromJsonToModel() {
        class ExampleModel(
                val int: Int,
                val string: String,
                val nullableLong: Long?,
                val nullableFloat: Float?
        )

        class ExampleModelMapper : Mapper<ExampleModel>() {

            val int = field(ExampleModel::int, "int_field")
            val string = field(ExampleModel::string, "string_field")
            val nullableLong = field(ExampleModel::nullableLong, "nullable_long_field")
            val nullableFloat = field(ExampleModel::nullableFloat, "nullable_float_field")

            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(
                        value of int, value of string, value of nullableLong, value of nullableFloat
                )
            }
        }

        val mapper = ExampleModelMapper()
        val expectedIntValue = 5
        val expectedStringValue = "test 6"
        val expectedNullableLongValue = null
        val expectedNullableFloatValue = 7f

        val jsonString = jsonString(
                mapper.int.name to expectedIntValue,
                mapper.string.name to expectedStringValue,
                mapper.nullableLong.name to expectedNullableLongValue,
                mapper.nullableFloat.name to expectedNullableFloatValue
        )


        val builtModel = mapper
                .buildJsonAdapter(Moshi.Builder().build())
                .fromJson(jsonReaderFrom(jsonString))

        assertThat(builtModel.int).isEqualTo(expectedIntValue)
        assertThat(builtModel.string).isEqualTo(expectedStringValue)
        assertThat(builtModel.nullableLong).isEqualTo(expectedNullableLongValue)
        assertThat(builtModel.nullableFloat).isEqualTo(expectedNullableFloatValue)
    }


    @Test
    fun canMapFromModelToJson() {
        class ExampleModel(
                val int: Int,
                val string: String,
                val nullableLong: Long?,
                val nullableFloat: Float?
        )

        class ExampleModelMapper : Mapper<ExampleModel>() {

            val int = field(ExampleModel::int, "int_field")
            val string = field(ExampleModel::string, "string_field")
            val nullableLong = field(ExampleModel::nullableLong, "nullable_long_field")
            val nullableFloat = field(ExampleModel::nullableFloat, "nullable_float_field")

            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(
                        value of int, value of string, value of nullableLong, value of nullableFloat
                )
            }
        }

        val mapper = ExampleModelMapper()
        val expectedIntValue = 5
        val expectedStringValue = "test 6"
        val expectedNullableLongValue = null
        val expectedNullableFloatValue = 7f

        val exampleModel = ExampleModel(
                expectedIntValue, expectedStringValue,
                expectedNullableLongValue, expectedNullableFloatValue
        )

        val stringSink = StringSink.create()
        mapper
                .buildJsonAdapter(Moshi.Builder().build())
                .toJson(jsonWriterTo(stringSink), exampleModel)

        val actualJsonString = stringSink.toString()
        val expectedJsonString = jsonString(
                mapper.int.name to expectedIntValue,
                mapper.string.name to expectedStringValue,
                mapper.nullableLong.name to expectedNullableLongValue,
                mapper.nullableFloat.name to expectedNullableFloatValue
        )

        assertThat(actualJsonString).isEqualTo(expectedJsonString)
    }


    @Test
    fun defaultNameFormatterIsCamelCaseToSnakeCaseFormatter() {
        class ExampleModel(val test: Int, val test1: Int, val testTest: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {

            val test = field(ExampleModel::test)
            val test1 = field(ExampleModel::test1)
            val testTest = field(ExampleModel::testTest)

            override fun create(value: Value<ExampleModel>): ExampleModel {
                throw UnsupportedOperationException("not implemented")
            }
        }

        val mapper = ExampleModelMapper()
        assertThat(mapper.test.name).isEqualTo("test")
        assertThat(mapper.test1.name).isEqualTo("test_1")
        assertThat(mapper.testTest.name).isEqualTo("test_test")
    }


    @Test
    fun explicitlyAssignedNameIsUsed() {
        val explicitFieldName = "explicit_int_field_name"
        data class ExampleModel(val int: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, explicitFieldName)
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int)
            }
        }

        val mapper = ExampleModelMapper()
        val exampleModel = ExampleModel(5)
        val jsonAdapterFromMapper = mapper.buildJsonAdapter(Moshi.Builder().build())

        // Check json from model
        val stringSink = StringSink.create()
        jsonAdapterFromMapper.toJson(jsonWriterTo(stringSink), exampleModel)
        val actualJsonString = stringSink.toString()
        val expectedJsonString = jsonString(explicitFieldName to exampleModel.int)
        assertThat(actualJsonString).isEqualTo(expectedJsonString)

        // Check model from json
        val builtModel = jsonAdapterFromMapper.fromJson(jsonReaderFrom(actualJsonString))
        assertThat(builtModel).isEqualTo(exampleModel)
    }


    @Test(expected = IllegalStateException::class)
    fun addingFieldTooLateThrows() {
        class ExampleModel(val int: Int, val string: String)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            init { field(ExampleModel::int) }
            override fun create(value: Value<ExampleModel>): ExampleModel {
                throw UnsupportedOperationException("not implemented")
            }
        }

        // Build adapter
        val mapper = ExampleModelMapper()
        mapper.buildJsonAdapter(Moshi.Builder().build())

        // Try to add another field now
        mapper.field(ExampleModel::string)
    }


    @Test
    fun canMapFromJsonWithExtraFieldsToModel() {
        class ExampleModel(val int: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int)
            }
        }

        val mapper = ExampleModelMapper()
        val expectedIntValue = 5

        val jsonString = jsonString(
                mapper.int.name to expectedIntValue,
                "extra_field_1" to "example value",
                "extra_field_2" to 42
        )

        val builtModel = mapper
                .buildJsonAdapter(Moshi.Builder().build())
                .fromJson(jsonReaderFrom(jsonString))

        assertThat(builtModel.int).isEqualTo(expectedIntValue)
    }


    @Test
    fun lastReadValuesAreClearedForFieldMappingsBeforeBuildingModelFromJson() {
        class ExampleModel(val int: Int, val nullable_string: String?)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            val nullable_string = field(ExampleModel::nullable_string, "nullable_string_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int, value of nullable_string)
            }
        }

        val mapper = ExampleModelMapper()
        val jsonAdapterFromMapper = mapper.buildJsonAdapter(Moshi.Builder().build())

        val jsonString1 = jsonString(
                mapper.int.name to 5,
                mapper.nullable_string.name to "non null value"
        )

        val builtModel1 = jsonAdapterFromMapper.fromJson(jsonReaderFrom(jsonString1))
        assertThat(builtModel1.int).isEqualTo(5)
        assertThat(builtModel1.nullable_string).isEqualTo("non null value")

        val jsonString2 = jsonString(
                mapper.int.name to 6,
                mapper.nullable_string.name to null
        )

        val builtModel2 = jsonAdapterFromMapper.fromJson(jsonReaderFrom(jsonString2))
        assertThat(builtModel2.int).isEqualTo(6)
        assertThat(builtModel2.nullable_string).isNull()
    }
}