plugins {
    application
    kotlin("jvm") version "2.0.21"
}


allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}


dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.11")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.1")
    implementation("org.mongodb:bson-kotlinx:5.0.1")
}

application {
    mainClass.set("core.MainKt")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "core.MainKt")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

kotlin.target.compilations.all {
    kotlinOptions {
        jvmTarget = "17"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(emptyList<File>())
        kotlin.setSrcDirs(listOf("sources"))
        resources.setSrcDirs(listOf("resources"))
    }

    getByName("test") {
        java.setSrcDirs(emptyList<File>())
        kotlin.setSrcDirs(listOf("tests"))
        resources.setSrcDirs(listOf("test resources"))
    }
}
