import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://jitpack.io") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.compose") version "0.2.0-build132"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "tk.zwander"
version = "1.0.0"

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("com.github.jengelman.gradle.plugins:shadow:1.2.3")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            description = "Permissions granter for SystemUI Tuner."
            vendor = "Zachary Wander"

            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            outputBaseDir.set(project.buildDir.resolve("out"))

            val icons = project.file("src/main/resources/images")
            windows {
                iconFile.set(icons.resolve("icon.ico"))
            }
            macOS {
                iconFile.set(icons.resolve("icon.icns"))
            }
            linux {
                iconFile.set(icons.resolve("icon.png"))
            }
        }
    }
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    manifest {
        attributes["Implementation-Title"] = "SUITGranter"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "MainKt"
    }

    archiveBaseName.set("${project.name}-${System.getProperty("os.name").replace(" ", "-").toLowerCase()}")
}

apply(plugin = "com.github.johnrengelman.shadow")