plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    api(project(":script-definition"))
    implementation("org.liquibase:liquibase-core:3.10.2")
    implementation(kotlin("stdlib-jdk8"))
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
