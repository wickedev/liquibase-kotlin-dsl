package org.liquibase.kotlin

fun databaseChangeLog(logicalFilePath: String? = null, closure: ((KotlinDatabaseChangeLog).() -> Unit)? = null): Pair<String?, ((KotlinDatabaseChangeLog).() -> Unit)?> {
	return logicalFilePath to closure
}