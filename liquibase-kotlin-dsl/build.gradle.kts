plugins {
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(project(":script-definition"))
    implementation("org.liquibase:liquibase-core:3.10.2")
}

publishing {
    publications {

        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        repositories {
            maven {
                url = uri("$rootDir/maven-repo")
            }
        }
    }
}