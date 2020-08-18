package org.liquibase.kotlin

class KotlinWhere {
	internal var where: String? = null
		private set

	fun where(where: String) {
		this.where = where
	}
}