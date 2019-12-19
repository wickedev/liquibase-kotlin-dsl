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
version = "1.2.3"

configurations {
    "archives"()
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("compiler"))
    implementation(kotlin("scripting-compiler"))
    implementation(kotlin("script-util"))
    implementation("org.liquibase:liquibase-core:3.4.2")
    testImplementation("junit:junit:4.12")
    testImplementation(kotlin("test"))
    testRuntimeOnly("com.h2database:h2:1.4.185")
    archives("org.apache.maven.wagon:wagon-ssh:2.8")
    archives("org.apache.maven.wagon:wagon-ssh-external:2.8")
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
