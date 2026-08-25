import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.aleksei"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // The IDE Config Doctor is built and sandbox-tested against.
        intellijIdeaCommunity("2024.2")

        // Config Doctor's whole purpose is analyzing YAML, so it depends on
        // the bundled YAML plugin for PSI access. No analyzer logic is
        // implemented at this stage - this is only the dependency wiring.
        bundledPlugin("org.jetbrains.plugins.yaml")

        // Standard IntelliJ Platform plugin tooling.
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = "Config Doctor"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "251.*"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
