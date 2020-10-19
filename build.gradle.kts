import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val kodeinVersion: String by project
val logbackVersion: String by project
val kotestVersion: String by project
val mockkVersion: String by project

plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    id("com.adarshr.test-logger") version "2.1.0"
    id("pl.allegro.tech.build.axion-release") version "1.12.1"
    jacoco
    application
}

scmVersion {
    tag.prefix = "v"
    tag.versionSeparator = ""
}

group = "org.postithere"
version = scmVersion.version

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property:$kotestVersion") // for kotest property test
    testImplementation("io.kotest:kotest-assertions-ktor:$kotestVersion") // for kotest property test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:mongodb:1.14.3")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.1.2")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.kodein.di:kodein-di:$kodeinVersion")
    implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:$kodeinVersion")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.configcat:configcat-android-client:5.+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}