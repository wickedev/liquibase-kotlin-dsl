package com.faendir.liquibase

databaseChangeLog {
    changeSet("test-1", Author.F43ND1R) {
        createTable("test") {
            column(name = "test", type = "LONGTEXT") {
                constraints(nullable = false)
            }
        }
    }
}