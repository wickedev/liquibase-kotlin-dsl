package org.liquibase.kotlin.helper

fun <T> assertType(value: Any, message: String? = null, expectations: ((T) -> Unit)? = null) {
	val casted = value as? T ?: throw AssertionError(message)

	expectations?.invoke(casted)
}