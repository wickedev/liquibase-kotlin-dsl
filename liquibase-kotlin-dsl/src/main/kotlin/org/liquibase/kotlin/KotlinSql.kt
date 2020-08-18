package org.liquibase.kotlin

import liquibase.changelog.DatabaseChangeLog

class KotlinSql(databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog) {
	internal var sql: String? = null
		private set
	internal var comment: String? = null
		private set

	fun comment(comment: String) {
		this.comment = comment
	}

	operator fun String.unaryMinus() {
		sql = this
	}
}