import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Generic form so platformType/platformVersion in gradle.properties
        // keep working exactly as they did with the 1.x plugin (IC, IU, etc.).
        create(properties("platformType"), properties("platformVersion"))

        // Comma-separated list in gradle.properties, e.g.
        // "com.intellij.java, org.jetbrains.plugins.yaml". Empty for now.
        plugins(properties("platformPlugins").map {
            it.split(',').map(String::trim).filter(String::isNotEmpty)
        })

        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            // untilBuild intentionally left unset (see gradle.properties) to
            // keep the plugin forward-compatible with newer IDE builds.
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}