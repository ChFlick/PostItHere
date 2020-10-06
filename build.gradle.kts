import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kodein_version: String by project
val logback_version: String by project
val kotest_version: String by project
val mockk_version: String by project

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    application
}

group = "org.postithere"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property:$kotest_version") // for kotest property test
    testImplementation("io.kotest:kotest-assertions-ktor:$kotest_version") // for kotest property test
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.mockk:mockk:$mockk_version")

    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.1.2")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.kodein.di:kodein-di:$kodein_version")
    implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:$kodein_version")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.configcat:configcat-android-client:5.+")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}