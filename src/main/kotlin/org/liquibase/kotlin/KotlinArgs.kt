package org.liquibase.kotlin

import liquibase.changelog.DatabaseChangeLog

class KotlinArgs(databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog) {
	internal val args: MutableList<String> = ArrayList()

	fun arg(arg: String) {
		args.add(arg.eval())
	}
}