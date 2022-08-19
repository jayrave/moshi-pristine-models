package com.jayrave.moshi.pristineModels

import com.jayrave.moshi.pristineModels.testLib.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.reflect.Type

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
                .getJsonAdapter(Moshi.Builder().build())
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
                .getJsonAdapter(Moshi.Builder().build())
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
    fun canMapModelWithPrivateProperties() {
        data class ExampleModel(private val int: Int, private val string: String?) {
            fun extractIntValue(): Int = int
            fun extractNullableStringValue(): String? = string
        }

        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field("int_field", false, object : PropertyExtractor<ExampleModel, Int> {
                override val type: Type = Int::class.javaPrimitiveType!!
                override fun extractFrom(t: ExampleModel): Int = t.extractIntValue()
            })

            val str = field("str_field", true, object : PropertyExtractor<ExampleModel, String?> {
                override val type: Type = String::class.javaObjectType
                override fun extractFrom(t: ExampleModel): String? = t.extractNullableStringValue()
            })

            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int, value of str)
            }
        }

        val mapper = ExampleModelMapper()
        val exampleModel = ExampleModel(5, null)
        val jsonAdapterFromMapper = mapper.getJsonAdapter(Moshi.Builder().build())

        // Check json from model
        val stringSink = StringSink.create()
        jsonAdapterFromMapper.toJson(jsonWriterTo(stringSink), exampleModel)
        val actualJsonString = stringSink.toString()
        assertThat(actualJsonString).isEqualTo(jsonString(
                mapper.int.name to exampleModel.extractIntValue(),
                mapper.str.name to exampleModel.extractNullableStringValue()
        ))

        // Check model from json
        val builtModel = jsonAdapterFromMapper.fromJson(jsonReaderFrom(actualJsonString))
        assertThat(builtModel).isEqualTo(exampleModel)
    }


    @Test
    fun fromJsonIsNullSafe() {
        class ExampleModel(val int: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int)
            }
        }

        val builtModel = ExampleModelMapper()
                .getJsonAdapter(Moshi.Builder().build())
                .fromJson("null")

        assertThat(builtModel).isNull()
    }


    @Test
    fun toJsonIsNullSafe() {
        class ExampleModel(val int: Int)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int)
            }
        }

        val stringSink = StringSink.create()
        ExampleModelMapper()
                .getJsonAdapter(Moshi.Builder().build())
                .toJson(jsonWriterTo(stringSink), null)

        assertThat(stringSink.toString()).isEqualTo("null")
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
        val jsonAdapterFromMapper = mapper.getJsonAdapter(Moshi.Builder().build())

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
        mapper.getJsonAdapter(Moshi.Builder().build())

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
                .getJsonAdapter(Moshi.Builder().build())
                .fromJson(jsonReaderFrom(jsonString))

        assertThat(builtModel.int).isEqualTo(expectedIntValue)
    }

    @Test(expected = JsonDataException::class)
    fun valueCanNotBeNullExceptionIsThrownForNonNullTypeIfValueIsNull() {
        class ExampleModel(val int: Int, val nullable_string: String)
        class ExampleModelMapper : Mapper<ExampleModel>() {
            val int = field(ExampleModel::int, "int_field")
            val nullable_string = field(ExampleModel::nullable_string, "nullable_string_field")
            override fun create(value: Value<ExampleModel>): ExampleModel {
                return ExampleModel(value of int, value of nullable_string)
            }
        }
        val mapper = ExampleModelMapper()
        val jsonAdapterFromMapper = mapper.getJsonAdapter(Moshi.Builder().build())
        val jsonString = jsonString(
            mapper.int.name to 1,
            mapper.nullable_string.name to null
        )
        jsonAdapterFromMapper.fromJson(jsonString)
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
        val jsonAdapterFromMapper = mapper.getJsonAdapter(Moshi.Builder().build())

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

    @Test
    fun parseNestedJson() {

        val jsonString = "users.json".readFileContent() ?: return Assertions.fail("users.json file not found")

        class Person(val name: String, val age: Int, val children: List<Person>?)

        class PersonMapper : Mapper<Person>() {
            val name = field(Person::name)
            val age = field(Person::age)

            val children = field(
                name = "children",
                valueCanBeNull = true,
                propertyExtractor = object : PropertyExtractor<Person, List<Person>?> {
                    override val type: Type
                        get() = Types.newParameterizedType(List::class.java, Person::class.java)

                    override fun extractFrom(t: Person): List<Person>? {
                        return t.children
                    }
                }
            )

            override fun create(value: Value<Person>): Person = Person(
                name = value of name,
                age = value of age,
                children = value of children
            )
        }

        val moshi = Moshi.Builder()
            .add(
                PristineModelsJsonAdapterFactory
                    .Builder()
                    .add(Person::class.java) { PersonMapper() }
                    .build()
            )
            .build()

        val personModel = moshi.adapter(Person::class.java).fromJson(jsonString)!!

        assert(personModel.name, "parent")
        assert(personModel.age, 25)
        assert(personModel.children!![0].name, "child 1")
        assert(personModel.children[0].age, 3)
        assert(personModel.children[1].name, "child 2")
        assert(personModel.children[1].age, 1)
    }

    private fun <T> assert(actual: T, expected: T) {
        kotlin.assert(actual == expected) {
            """
                Expected value: $expected
                Actual value: $actual
            """
        }
    }
}