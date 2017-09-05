package org.liquibase.kotlin

import liquibase.change.ColumnConfig
import liquibase.changelog.DatabaseChangeLog

class KotlinDropColumn(databaseChangeLog: DatabaseChangeLog) : KotlinColumn<ColumnConfig>(databaseChangeLog, { ColumnConfig() }) {
	fun column(name: String) {
		super.column(name, null, null, null, null, null, null,
				null, null, null, null,
				null, null, null, null, null,
				null, null, null, null, null,
				null, null, null)
	}
}