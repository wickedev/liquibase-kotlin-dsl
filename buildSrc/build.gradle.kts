plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    gradlePluginPortal()
}

dependencies {
    val bintrayPluginVersion: String by project
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:$bintrayPluginVersion")
    implementation(kotlin("gradle-plugin"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}