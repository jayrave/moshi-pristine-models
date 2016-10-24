package com.jayrave.moshi.pristineModels

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CamelCaseToSnakeCaseFormatterTest {

    private val formatter: NameFormatter = CamelCaseToSnakeCaseFormatter()

    @Test
    fun noOp() {
        assertThat(formatter.format("test")).isEqualTo("test")
    }

    @Test
    fun withOneConnector() {
        assertThat(formatter.format("testTest")).isEqualTo("test_test")
    }

    @Test
    fun withMultipleConnectors() {
        assertThat(formatter.format("testTestTestTest")).isEqualTo("test_test_test_test")
    }

    @Test
    fun withSingleDigit() {
        assertThat(formatter.format("test1")).isEqualTo("test_1")
    }

    @Test
    fun withMultipleDigits() {
        assertThat(formatter.format("test123")).isEqualTo("test_123")
    }

    @Test
    fun withMixedDigitsAndTest() {
        assertThat(formatter.format("test123Test4Test56Test789")).isEqualTo(
                "test_123_test_4_test_56_test_789"
        )
    }

    @Test(expected = NameFormatter.NameFormatException::class)
    fun nameFormatExceptionIsThrownIfInputIsEmpty() {
        formatter.format("")
    }
}