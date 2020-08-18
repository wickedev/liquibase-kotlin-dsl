plugins {
    publish
}

dependencies {
    api(project(":script-definition"))
    implementation("org.liquibase:liquibase-core:3.10.2")
}