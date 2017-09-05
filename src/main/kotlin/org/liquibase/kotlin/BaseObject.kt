package org.liquibase.kotlin

import liquibase.changelog.DatabaseChangeLog
import liquibase.statement.DatabaseFunction
import liquibase.statement.SequenceCurrentValueFunction
import liquibase.statement.SequenceNextValueFunction
import java.math.BigInteger

abstract class BaseObject(internal val databaseChangeLog: DatabaseChangeLog) {
	protected fun Any.eval(): String {
		return databaseChangeLog.changeLogParameters.expandExpressions(this.toString(), databaseChangeLog)
	}

	protected fun Any.evalBool(): Boolean {
		if (this is Boolean) {
			return this
		}

		val exp = this.toString().eval()
		return exp.toBoolean()
	}

	protected fun Any.evalInt(): Int? {
		if (this is Number) {
			return this.toInt()
		}

		val exp = this.toString().eval()
		return exp.toIntOrNull()
	}

	protected fun Any.evalBigInteger(): BigInteger? {
		if (this is Number) {
			return BigInteger.valueOf(this.toLong())
		}

		val exp = this.toString().eval()
		return exp.toLongOrNull()?.let { BigInteger.valueOf(it) }
	}

	protected fun Any.evalDatabaseFunction(): DatabaseFunction {
		if (this is DatabaseFunction) {
			return this
		}

		val exp = this.toString().eval()
		return DatabaseFunction(exp)
	}

	protected fun Any.evalNextSequence(): SequenceNextValueFunction {
		if (this is SequenceNextValueFunction) {
			return this
		}

		val exp = this.toString().eval()
		return SequenceNextValueFunction(exp)
	}

	protected fun Any.evalCurrentSequence(): SequenceCurrentValueFunction {
		if (this is SequenceCurrentValueFunction) {
			return this
		}

		val exp = this.toString().eval()
		return SequenceCurrentValueFunction(exp)
	}
}