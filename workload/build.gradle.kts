plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":processor"))
    ksp(project(":processor"))
}

ksp {
    arg("PROD", "prod")
    arg("TEST", "test")
    arg("DEV", "dev")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}