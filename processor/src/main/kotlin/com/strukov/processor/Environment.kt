package com.strukov.processor

@Target(AnnotationTarget.PROPERTY)
annotation class Url(
    val environment: Environment,
    val name: String
)

enum class Environment(val env: String) {
    PROD("prod"),
    TEST("test"),
    DEV("dev")
}
