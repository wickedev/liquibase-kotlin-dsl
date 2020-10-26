plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    implementation(kotlin("scripting-jvm"))
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
