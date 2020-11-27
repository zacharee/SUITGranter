import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.compose") version "0.2.0-build131"
}

group = "tk.zwander"
version = "1.0.0"

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
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