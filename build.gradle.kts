import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
    }
}

plugins {
    kotlin("jvm") version "1.3.61"
    `maven-publish`
    idea
}

group = "com.faendir.liquibase"
version = "2.0.0"

configurations {
    "archives"()
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.liquibase:liquibase-core:3.4.2")
    implementation(kotlin("stdlib-jdk8"))
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        mavenLocal()
        maven {
            setUrl("https://api.bintray.com/maven/f43nd1r/maven/liquibase-kotlin-dsl/;publish=1")
            name = "bintray"
            credentials {
                username = findProperty("artifactoryUser") as String?
                password = findProperty("artifactoryApiKey") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar.get())
            pom {
                name.set("liquibase-kotlin-dsl")
                description.set("kotlin dsl plugin for liquibase")
                url.set("https://github.com/F43nd1r/liquibase-kotlin-dsl")

                scm {
                    connection.set("scm:git:https://github.com/F43nd1r/liquibase-kotlin-dsl.git")
                    developerConnection.set("scm:git:git@github.com:F43nd1r/liquibase-kotlin-dsl.git")
                    url.set("https://github.com/F43nd1r/liquibase-kotlin-dsl.git")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("f43nd1r")
                        name.set("Lukas Morawietz")
                    }
                }
            }
        }
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}