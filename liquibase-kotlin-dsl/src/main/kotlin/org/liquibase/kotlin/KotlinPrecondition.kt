package org.liquibase.kotlin

import liquibase.changelog.DatabaseChangeLog
import liquibase.precondition.CustomPreconditionWrapper
import liquibase.precondition.PreconditionLogic
import liquibase.precondition.core.*
import java.io.Serializable

class KotlinPrecondition(onFail: PreconditionContainer.FailOption?,
						 onError: PreconditionContainer.ErrorOption?,
						 onUpdateSQL: PreconditionContainer.OnSqlOutputOption?,
						 onFailMessage: String? = null,
						 onErrorMessage: String? = null,
						 databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog), Serializable {

	internal val preconditions = PreconditionContainer()

	init {
		preconditions.setOnFail(onFail?.name)
		preconditions.setOnError(onError?.name)
		preconditions.setOnSqlOutput(onUpdateSQL?.name)

		preconditions.onFailMessage = onFailMessage
		preconditions.onErrorMessage = onErrorMessage
	}

	fun dbms(type: String) {
		preconditions.addNestedPrecondition(DBMSPrecondition().apply { this.type = type.eval() })
	}

	fun runningAs(username: String) {
		preconditions.addNestedPrecondition(RunningAsPrecondition().apply { this.username = username.eval() })
	}

	fun changeSetExecuted(id: String, author: String, changeLogFile: String) {
		preconditions.addNestedPrecondition(ChangeSetExecutedPrecondition().apply {
			this.id = id.eval()
			this.author = author.eval()
			this.changeLogFile = changeLogFile.eval()
		})
	}

	fun columnExists(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null) {
		preconditions.addNestedPrecondition(ColumnExistsPrecondition().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun sqlCheck(expectedResult: Any, sql: () -> String) {
		preconditions.addNestedPrecondition(SqlPrecondition().apply {
			this.expectedResult = expectedResult.eval()
			this.sql = sql().eval()
		})
	}

	fun tableExists(tableName: String, catalogName: String? = null, schemaName: String? = null) {
		preconditions.addNestedPrecondition(TableExistsPrecondition().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun viewExists(viewName: String, catalogName: String? = null, schemaName: String? = null) {
		preconditions.addNestedPrecondition(ViewExistsPrecondition().apply {
			this.viewName = viewName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun foreignKeyConstraintExists(foreignKeyName: String, foreignKeyTableName: String? = null, catalogName: String? = null,
								   schemaName: String? = null) {
		preconditions.addNestedPrecondition(ForeignKeyExistsPrecondition().apply {
			this.foreignKeyName = foreignKeyName.eval()
			this.foreignKeyTableName = foreignKeyTableName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName
		})
	}

	fun indexExists(indexName: String, tableName: String? = null, catalogName: String? = null, schemaName: String? = null,
					columnNames: String? = null) {
		preconditions.addNestedPrecondition(IndexExistsPrecondition().apply {
			this.indexName = indexName.eval()
			this.tableName = tableName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.columnNames = columnNames?.eval()
		})
	}

	fun sequenceExists(sequenceName: String, schemaName: String? = null, catalogName: String? = null) {
		preconditions.addNestedPrecondition(SequenceExistsPrecondition().apply {
			this.sequenceName = sequenceName.eval()
			this.schemaName = schemaName?.eval()
			this.catalogName = catalogName?.eval()
		})
	}

	fun primaryKeyExists(tableName: String? = null, primaryKeyName: String? = null, catalogName: String? = null,
						 schemaName: String? = null) {
		preconditions.addNestedPrecondition(PrimaryKeyExistsPrecondition().apply {
			this.tableName = tableName?.eval()
			this.primaryKeyName = primaryKeyName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun customPrecondition(className: String, params: ((KotlinParameterWrapper).() -> Unit)? = null) {
		val precondition = CustomPreconditionWrapper().apply {
			this.classLoader = this@KotlinPrecondition.javaClass.classLoader
			this.className = className.eval()
		}

		if (params != null) {
			val wrapper = KotlinParameterWrapper(databaseChangeLog)
			wrapper.params()

			wrapper.params.forEach { name, value ->
				precondition.setParam(name, value)
			}
		}

		preconditions.addNestedPrecondition(precondition)
	}

	fun and(closure: (KotlinPrecondition).() -> Unit) {
		nestedPrecondition(AndPrecondition(), closure)
	}

	fun or(closure: (KotlinPrecondition).() -> Unit) {
		nestedPrecondition(OrPrecondition(), closure)
	}

	fun not(closure: (KotlinPrecondition).() -> Unit) {
		nestedPrecondition(NotPrecondition(), closure)
	}

	private fun nestedPrecondition(precondition: PreconditionLogic, closure: (KotlinPrecondition).() -> Unit) {
		val condition = KotlinPrecondition(null, null, null, null, null, databaseChangeLog)
		condition.closure()

		condition.preconditions.nestedPreconditions.forEach(precondition::addNestedPrecondition)

		preconditions.addNestedPrecondition(precondition)
	}
}