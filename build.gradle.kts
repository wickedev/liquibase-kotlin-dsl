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

// Release version that won't conflict with the bintray plugin
group = "org.liquibase"
version = "1.2.2"

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
    }
    publications {
        register("maven", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
