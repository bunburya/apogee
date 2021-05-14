plugins {
    java
    kotlin("jvm") version "1.4.32"
    application
}

application {
    mainClass.set("eu.bunburya.apogee.MainKt")
}

group = "eu.bunburya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
    // https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty", "netty-all", "4.1.63.Final")
    // https://mvnrepository.com/artifact/com.moandjiezana.toml/toml4j
    implementation("com.moandjiezana.toml", "toml4j", "0.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}