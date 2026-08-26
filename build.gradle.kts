import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
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
        create(properties("platformType"), properties("platformVersion"))
        bundledPlugin("org.jetbrains.plugins.yaml")
        plugins(properties("platformPlugins").map {
            it.split(',').map(String::trim).filter(String::isNotEmpty)
        })

        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

        // NEW - workaround for JetBrains-acknowledged bugs in
        // TestFrameworkType.Platform (IntelliJ Platform Gradle Plugin 2.x):
        //   - IJPL-159134: junit.framework.TestCase not resolved
        //     (BasePlatformTestCase needs it - this is what broke our test).
        //   - IJPL-157292: opentest4j not resolved.
        // See: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html
        testImplementation("junit:junit:4.13.2")
        testImplementation("org.opentest4j:opentest4j:1.3.0")
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