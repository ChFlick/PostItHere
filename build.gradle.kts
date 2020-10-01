import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}
group = "org.postithere"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    implementation("io.ktor:ktor-server-netty:1.4.1")
    implementation("io.ktor:ktor-html-builder:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.1.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}