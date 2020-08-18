package org.liquibase.kotlin

import liquibase.changelog.DatabaseChangeLog

class KotlinParameterWrapper(databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog) {
	internal val params: MutableMap<String, String> = HashMap()

	fun param(name: String, value: Any) {
		params[name] = value.eval()
	}
}