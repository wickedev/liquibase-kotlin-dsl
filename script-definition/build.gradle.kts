plugins {
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(kotlin("scripting-jvm"))
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