plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

tasks.withType<org.gradle.plugin.devel.tasks.ValidatePlugins>().configureEach {
    enabled = false
}
