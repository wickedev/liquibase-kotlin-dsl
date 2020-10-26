/*plugins {
    id("maven-publish")
}*/

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
    // Ensure "org.gradle.jvm.version" is set to "8" in Gradle metadata.
    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}