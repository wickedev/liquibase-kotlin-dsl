plugins {
    id("org.springframework.boot") version "2.3.3.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm")
    kotlin("plugin.spring") version "1.3.72"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("com.h2database:h2")
    implementation(project(":liquibase-kotlin-dsl"))
}
