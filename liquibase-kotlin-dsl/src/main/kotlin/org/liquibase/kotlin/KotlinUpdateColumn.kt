package org.liquibase.kotlin

import liquibase.change.ColumnConfig
import liquibase.changelog.DatabaseChangeLog

class KotlinUpdateColumn(databaseChangeLog: DatabaseChangeLog) : KotlinColumn<ColumnConfig>(databaseChangeLog, { ColumnConfig() }) {
	internal var where: String? = null
		private set

	fun where(where: String) {
		this.where = where.eval()
	}
}