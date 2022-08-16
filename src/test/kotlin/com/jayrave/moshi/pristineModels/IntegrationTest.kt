package com.jayrave.moshi.pristineModels

import com.jayrave.moshi.pristineModels.testLib.readFileContent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.assertj.core.api.Assertions
import org.junit.Test
import java.lang.reflect.Type

class IntegrationTest {

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
        assert(personModel.children!![0].age, 3)
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