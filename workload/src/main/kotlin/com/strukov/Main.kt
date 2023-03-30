package com.strukov

import com.strukov.processor.Environment
import com.strukov.processor.EnvironmentConfig
import com.strukov.processor.EnvironmentSettings
import com.strukov.processor.UrlPrinter
import com.strukov.processor.Url

internal fun main() {
    SampleUrlPrinter().print()
}

@UrlPrinter
internal interface SampleUrl {
    val sampleConfigUrl: SampleConfigUrl
}

@EnvironmentSettings
internal interface SampleEnvironment

@EnvironmentConfig
internal interface SampleConfig {
    @Url(environment = Environment.PROD, name = "https://www.prod.com")
    val prod: String

    @Url(environment = Environment.TEST, name = "https://www.test.com")
    val test: String
}