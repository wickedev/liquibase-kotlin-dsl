package org.liquibase.kotlin

import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import liquibase.resource.FileSystemResourceAccessor

interface KotlinDatabaseChangeLogDefinition {
    fun define() : DatabaseChangeLog


    fun changeLog(closure: (KotlinDatabaseChangeLog).() -> Unit): DatabaseChangeLog {
        val changelog = DatabaseChangeLog(this::class.java.name)
        changelog.changeLogParameters = ChangeLogParameters()
        val ktChangeLog = KotlinDatabaseChangeLog(changelog)
        ktChangeLog.resourceAccessor = FileSystemResourceAccessor()
        ktChangeLog.closure()
        return changelog
    }
}