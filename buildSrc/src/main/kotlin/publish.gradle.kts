import org.gradle.api.publish.maven.internal.publication.DefaultMavenPom

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks.register<Jar>("sourcesJar") {
    group = "documentation"
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
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
    repositories {
        mavenLocal()
    }
}

bintray {
    val bintrayUser: String? by project
    val bintrayPassword: String? by project
    user = bintrayUser
    key = bintrayPassword
    setPublications("maven")
    publish = true
    pkg.apply {
        repo = "maven"
        afterEvaluate {
            val pom = (publishing.publications["maven"] as MavenPublication).pom as DefaultMavenPom
            this@apply.name = pom.name.get()
            websiteUrl = pom.url.get()
            vcsUrl = pom.scm.url.get()
            setLicenses(*pom.licenses.map { it.name.get() }.toTypedArray())
            desc = pom.description.get()
        }
        publicDownloadNumbers = true
        version.apply {
            name = project.version.toString()
        }
    }
}
tasks["publish"].dependsOn("bintrayUpload")
