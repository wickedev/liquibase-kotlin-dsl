package org.liquibase.kotlin

import liquibase.Labels
import liquibase.change.AbstractChange
import liquibase.change.AddColumnConfig
import liquibase.change.ColumnConfig
import liquibase.change.core.*
import liquibase.change.custom.CustomChangeWrapper
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.database.ObjectQuotingStrategy
import liquibase.exception.RollbackImpossibleException
import liquibase.precondition.core.PreconditionContainer
import liquibase.resource.ResourceAccessor
import java.io.Serializable

class KotlinChangeSet(
		id: String,
		author: String,
		runAlways: Boolean,
		runOnChange: Boolean,
		context: String? = null,
		dbms: String? = null,
		labels: String? = null,
		runInTransaction: Boolean,
		databaseChangeLog: DatabaseChangeLog,
		failOnError: Boolean? = null,
		onValidationFail: ChangeSet.ValidationFailOption? = null,
		objectQuotingStrategy: ObjectQuotingStrategy? = null,
		addToChangelog: Boolean = true) : BaseObject(databaseChangeLog), Serializable {

	internal var resourceAccessor: ResourceAccessor? = null

	internal val changeSet = ChangeSet(id, author, runAlways, runOnChange, databaseChangeLog.filePath, context, dbms,
			runInTransaction, objectQuotingStrategy, databaseChangeLog)

	init {
		if (labels != null) {
			changeSet.labels = Labels(labels)
		}

		changeSet.failOnError = failOnError

		if (onValidationFail != null) {
			changeSet.onValidationFail = onValidationFail
		}

		if (addToChangelog) {
			databaseChangeLog.addChangeSet(changeSet)
		}
	}

	fun comment(text: String) {
		if (changeSet.comments == null) {
			changeSet.comments = text
		} else {
			changeSet.comments += " $text"
		}
	}

	fun comment(comment: () -> String) {
		comment(comment())
	}

	fun preConditions(onFail: PreconditionContainer.FailOption? = null,
					  onError: PreconditionContainer.ErrorOption? = null,
					  onOutputSql: PreconditionContainer.OnSqlOutputOption? = null,
					  closure: ((KotlinPrecondition).() -> Unit)? = null) {
		val ktPreconditions = KotlinPrecondition(onFail, onError, onOutputSql, null, null, databaseChangeLog)

		closure?.let {
			ktPreconditions.it()
		}

		changeSet.preconditions = ktPreconditions.preconditions
	}

	fun rollback(changeSetId: String? = null, changeSetAuthor: String? = null, sql: String? = null, changeSetPath: String? = null) {
		if (sql != null) {
			changeSet.addRollBackSQL(sql.eval())
		} else {
			if (changeSetId == null) {
				throw RollbackImpossibleException("no changeSetId given for rollback in ${changeSet.id}")
			}

			val author = changeSetAuthor?.eval()
			val filePath = changeSetPath?.eval() ?: databaseChangeLog.filePath
			val id = changeSetId.eval()

			val referencedChangeSet = databaseChangeLog.getChangeSet(filePath, author, id)
					?: throw RollbackImpossibleException("Could not find changeSet to use for rollback: $filePath:$author:$id")

			referencedChangeSet.changes.forEach(changeSet::addRollbackChange)
		}
	}

	fun rollback(closure: (KotlinChangeSet).() -> Unit) {
		val rollbackCS = KotlinChangeSet(
				"",
				"",
				false,
				false,
				runInTransaction = true,
				databaseChangeLog = databaseChangeLog,
				addToChangelog = false)

		rollbackCS.closure()

		rollbackCS.changeSet.changes.forEach { changeSet.addRollbackChange(it) }
	}

	fun update(tableName: String, catalogName: String? = null, schemaName: String? = null,
							   closure: (KotlinUpdateColumn.() -> Unit)? = null) {
		val updateChange = UpdateDataChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		}

		if (closure != null) {
			val kotlinColumn = KotlinUpdateColumn(databaseChangeLog)
			kotlinColumn.closure()

			updateChange.where = kotlinColumn.where

			kotlinColumn.columns.forEach {
				updateChange.addColumn(it)
			}
		}

		addChange(updateChange)
	}

	fun delete(tableName: String, catalogName: String? = null, schemaName: String? = null, where: ((KotlinWhere).() -> Unit)? = null) {
		addChange(DeleteDataChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.where = where?.let {
				KotlinWhere().apply {
					where()
				}.where?.eval()
			}
		})
	}

	fun insert(tableName: String, catalogName: String? = null, schemaName: String? = null, dbms: String? = null,
							   columns: (KotlinColumn<ColumnConfig>).() -> Unit) {
		val insertChange = InsertDataChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.dbms = dbms?.eval()
		}

		val kotlinColumn = KotlinColumn(databaseChangeLog) { ColumnConfig() }
		kotlinColumn.columns()

		kotlinColumn.columns.forEach {
			insertChange.addColumn(it)
		}

		addChange(insertChange)
	}

	fun loadData(tableName: String, file: String, catalogName: String? = null, schemaName: String? = null,
								 relativeToChangelogFile: Any? = null, encoding: String? = null, separator: String? = null,
								 quotchar: String? = null, columns: (KotlinLoadDataColumn).() -> Unit) {
		val loadDataChange = LoadDataChange().apply {
			this.tableName = tableName.eval()
			this.file = file.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.isRelativeToChangelogFile = relativeToChangelogFile?.evalBool()
			this.encoding = encoding?.eval()

			separator?.let {
				this.separator = it.eval()
			}
			quotchar?.let {
				this.quotchar = it.eval()
			}
		}

		val kotlinColumn = KotlinLoadDataColumn(databaseChangeLog)
		kotlinColumn.columns()

		kotlinColumn.columns.forEach {
			loadDataChange.addColumn(it)
		}

		addChange(loadDataChange)
	}

	fun loadUpdateData(tableName: String, file: String, primaryKey: String, catalogName: String? = null, schemaName: String? = null,
									   relativeToChangelogFile: Any? = null, encoding: String? = null, separator: String? = null,
									   quotchar: String? = null, onlyUpdate: Any? = null, columns: (KotlinLoadDataColumn).() -> Unit) {
		val loadUpdateDataChange = LoadUpdateDataChange().apply {
			this.tableName = tableName.eval()
			this.file = file.eval()
			this.primaryKey = primaryKey.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.isRelativeToChangelogFile = relativeToChangelogFile?.evalBool()
			this.encoding = encoding?.eval()

			separator?.let {
				this.separator = it.eval()
			}
			quotchar?.let {
				this.quotchar = it.eval()
			}
			onlyUpdate?.let {
				this.onlyUpdate = it.evalBool()
			}
		}

		val kotlinColumn = KotlinLoadDataColumn(databaseChangeLog)
		kotlinColumn.columns()

		kotlinColumn.columns.forEach {
			loadUpdateDataChange.addColumn(it)
		}

		addChange(loadUpdateDataChange)
	}

	fun addColumn(tableName: String, catalogName: String? = null, schemaName: String? = null,
				  closure: (KotlinColumn<AddColumnConfig>.() -> Unit)? = null) {
		val addColumnChange = AddColumnChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		}

		if (closure != null) {
			val kotlinColumn = KotlinColumn(databaseChangeLog) { AddColumnConfig() }
			kotlinColumn.closure()

			kotlinColumn.columns.forEach {
				addColumnChange.addColumn(it)
			}
		}

		addChange(addColumnChange)
	}

	fun empty() {
		addChange(EmptyChange())
	}

	fun renameTable(oldTableName: String, newTableName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(RenameTableChange().apply {
			this.oldTableName = oldTableName.eval()
			this.newTableName = newTableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun validCheckSum(checksum: String) {
		changeSet.addValidCheckSum(checksum)
	}

	fun dropColumn(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null,
				   columns: ((KotlinDropColumn).() -> Unit)? = null) {
		addChange(DropColumnChange().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()

			columns?.let {
				KotlinDropColumn(databaseChangeLog).apply {
					columns()
				}.columns.forEach(this::addColumn)
			}
		})
	}

	fun createIndex(tableName: String, indexName: String? = null, catalogName: String? = null, schemaName: String? = null,
					tablespace: String? = null, unique: Any? = null, clustered: Any? = null,
					associatedWith: String? = null, columns: ((KotlinAddColumn).() -> Unit)? = null) {
		val createIndexChange = CreateIndexChange().apply {
			this.tableName = tableName.eval()
			this.indexName = indexName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tablespace = tablespace?.eval()
			this.isUnique = unique?.evalBool()
			this.clustered = clustered?.evalBool()
			this.associatedWith = associatedWith?.eval()
		}

		if (columns != null) {
			val kotlinColumn = KotlinAddColumn(databaseChangeLog)
			kotlinColumn.columns()

			kotlinColumn.columns.forEach(createIndexChange::addColumn)
		}

		addChange(createIndexChange)
	}

	fun dropIndex(indexName: String, tableName: String? = null, catalogName: String? = null, schemaName: String? = null,
				  associatedWith: String? = null) {
		addChange(DropIndexChange().apply {
			this.indexName = indexName.eval()
			this.tableName = tableName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.associatedWith = associatedWith?.eval()
		})
	}

	fun addAutoIncrement(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null,
						 columnDataType: String? = null, incrementBy: Any? = null, startWith: Any? = null) {
		addChange(AddAutoIncrementChange().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.columnDataType = columnDataType?.eval()
			this.incrementBy = incrementBy?.evalBigInteger()
			this.startWith = startWith?.evalBigInteger()
		})
	}

	fun addDefaultValue(tableName: String, columnName: String, catalogName: String? = null, columnDataType: String? = null,
						defaultValue: String? = null, defaultValueBoolean: Any? = null, defaultValueComputed: Any? = null,
						defaultValueDate: String? = null, defaultValueNumeric: String? = null, defaultValueSequenceNext: Any? = null,
						schemaName: String? = null) {
		addChange(AddDefaultValueChange().apply {
			this.catalogName = catalogName?.eval()
			this.columnDataType = columnDataType?.eval()
			this.columnName = columnName.eval()
			this.defaultValue = defaultValue?.eval()
			this.defaultValueBoolean = defaultValueBoolean?.evalBool()
			this.defaultValueComputed = defaultValueComputed?.evalDatabaseFunction()
			this.defaultValueDate = defaultValueDate?.eval()
			this.defaultValueNumeric = defaultValueNumeric?.eval()
			this.defaultValueSequenceNext = defaultValueSequenceNext?.evalNextSequence()
			this.schemaName = schemaName?.eval()
			this.tableName = tableName.eval()
		})
	}

	fun addLookupTable(existingColumnName: String, existingTableName: String, existingTableCatalogName: String? = null,
					   existingTableSchemaName: String? = null, newColumnName: String, newTableName: String,
					   newTableCatalogName: String? = null, newTableSchemaName: String? = null,
					   newColumnDataType: String? = null, constraintName: String? = null) {
		addChange(AddLookupTableChange().apply {
			this.existingColumnName = existingColumnName.eval()
			this.existingTableName = existingTableName.eval()
			this.existingTableCatalogName = existingTableCatalogName?.eval()
			this.existingTableSchemaName = existingTableSchemaName?.eval()
			this.newColumnName = newColumnName.eval()
			this.newTableName = newTableName.eval()
			this.newTableCatalogName = newTableCatalogName?.eval()
			this.newTableSchemaName = newTableSchemaName?.eval()
			this.newColumnDataType = newColumnDataType?.eval()
			this.constraintName = constraintName?.eval()
		})
	}

	fun addNotNullConstraint(tableName: String, columnName: String, constraintName: String? = null, catalogName: String? = null, schemaName: String? = null,
							 defaultNullValue: String? = null, columnDataType: String? = null) {
		addChange(AddNotNullConstraintChange().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.constraintName = constraintName?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.defaultNullValue = defaultNullValue?.eval()
			this.columnDataType = columnDataType?.eval()
		})
	}

	fun addUniqueConstraint(tableName: String, columnNames: String, tablespace: String? = null, catalogName: String? = null,
							schemaName: String? = null, constraintName: String? = null, deferrable: Any? = null,
							disabled: Any? = null, initiallyDeferred: Any? = null, forIndexName: String? = null,
							forIndexSchemaName: String? = null, forIndexCatalogName: String? = null) {
		addChange(AddUniqueConstraintChange().apply {
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tableName = tableName.eval()
			this.columnNames = columnNames.eval()
			this.constraintName = constraintName?.eval()
			this.tablespace = tablespace?.eval()
			this.deferrable = deferrable?.evalBool()
			this.initiallyDeferred = initiallyDeferred?.evalBool()
			this.disabled = disabled?.evalBool()
			this.forIndexName = forIndexName?.eval()
			this.forIndexSchemaName = forIndexSchemaName?.eval()
			this.forIndexCatalogName = forIndexCatalogName?.eval()
		})
	}

	fun alterSequence(sequenceName: String, catalogName: String? = null, schemaName: String? = null, incrementBy: Any? = null,
					  maxValue: Any? = null, minValue: Any? = null, ordered: Any? = null, cacheSize: Any? = null,
					  willCycle: Any? = null) {
		addChange(AlterSequenceChange().apply {
			this.sequenceName = sequenceName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.incrementBy = incrementBy?.evalBigInteger()
			this.maxValue = maxValue?.evalBigInteger()
			this.minValue = minValue?.evalBigInteger()
			this.isOrdered = ordered?.evalBool()
			this.cacheSize = cacheSize?.evalBigInteger()
			this.willCycle = willCycle?.evalBool()
		})
	}

	fun createSequence(sequenceName: String, catalogName: String? = null, schemaName: String? = null, incrementBy: Any? = null,
					   startValue: Any? = null, maxValue: Any? = null, minValue: Any? = null, ordered: Any? = null,
					   cacheSize: Any? = null, cycle: Any? = null) {
		addChange(CreateSequenceChange().apply {
			this.sequenceName = sequenceName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.incrementBy = incrementBy?.evalBigInteger()
			this.startValue = startValue?.evalBigInteger()
			this.maxValue = maxValue?.evalBigInteger()
			this.minValue = minValue?.evalBigInteger()
			this.isOrdered = ordered?.evalBool()
			this.cacheSize = cacheSize?.evalBigInteger()
			this.cycle = cycle?.evalBool()
		})
	}

	fun dropDefaultValue(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null,
						 columnDataType: String? = null) {
		addChange(DropDefaultValueChange().apply {
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.columnDataType = columnDataType?.eval()
		})
	}

	fun dropNotNullConstraint(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null,
						 columnDataType: String? = null) {
		addChange(DropNotNullConstraintChange().apply {
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.columnDataType = columnDataType?.eval()
		})
	}

	fun dropSequence(sequenceName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(DropSequenceChange().apply {
			this.sequenceName = sequenceName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun dropUniqueConstraint(tableName: String, constraintName: String, catalogName: String? = null, schemaName: String? = null,
							 uniqueColumns: String? = null) {
		addChange(DropUniqueConstraintChange().apply {
			this.tableName = tableName.eval()
			this.constraintName = constraintName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.uniqueColumns = uniqueColumns?.eval()
		})
	}

	fun renameSequence(oldSequenceName: String, newSequenceName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(RenameSequenceChange().apply {
			this.oldSequenceName = oldSequenceName.eval()
			this.newSequenceName = newSequenceName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun customChange(className: String, params: ((KotlinParameterWrapper).() -> Unit)? = null) {
		val customChangeWrapper = CustomChangeWrapper().apply {
			classLoader = KotlinChangeSet@this.javaClass.classLoader
			setClass(className.eval())
		}

		if (params != null) {
			val paramWrapper = KotlinParameterWrapper(databaseChangeLog)
			paramWrapper.params()
			paramWrapper.params.forEach { name, value ->
				customChangeWrapper.setParam(name, value)
			}
		}

		addChange(customChangeWrapper)
	}

	fun executeCommand(executable: String, os: String? = null, args: ((KotlinArgs).() -> Unit)? = null) {
		val executeCommand = ExecuteShellCommandChange().apply {
			this.executable = executable
			this.setOs(os)
		}

		if (args != null) {
			val paramWrapper = KotlinArgs(databaseChangeLog)
			paramWrapper.args()
			paramWrapper.args.forEach(executeCommand::addArg)
		}

		addChange(executeCommand)
	}

	fun modifySql(dbms: String? = null, context: String? = null, labels: String? = null, applyToRollback: Any? = null,
			configure: (KotlinModifySql).() -> Unit) {
		val modifySql = KotlinModifySql(dbms, context, labels, applyToRollback, databaseChangeLog)
		modifySql.configure()

		modifySql.sqlVisitors.forEach(changeSet::addSqlVisitor)
	}

	fun sql(sql: String) {
		addChange(RawSQLChange().apply {
			this.sql = sql.eval()
		})
	}

	fun sql(stripComments: Any? = null, splitStatements: Any? = null, endDelimiter: String? = null, dbms: String? = null,
			sql: (KotlinSql).() -> Unit) {
		val change = KotlinSql(databaseChangeLog)
		change.sql()

		addChange(RawSQLChange().apply {
			this.sql = change.sql
			this.comment = change.comment
			this.isStripComments = stripComments?.evalBool()
			this.isSplitStatements = splitStatements?.evalBool()
			this.endDelimiter = endDelimiter?.eval()
			this.dbms = dbms?.eval()
		})
	}

	fun sqlFile(path: String, relativeToChangelogFile: Any? = null, stripComments: Any? = null,
				splitStatements: Any? = null, endDelimiter: String? = null, dbms: String? = null, encoding: String? = null) {
		addChange(SQLFileChange().apply {
			this.path = path
			this.isRelativeToChangelogFile = relativeToChangelogFile?.evalBool()
			this.isStripComments = stripComments?.evalBool()
			this.isSplitStatements = splitStatements?.evalBool()
			this.endDelimiter = endDelimiter?.eval()
			this.dbms = dbms?.eval()
			this.encoding = encoding?.eval()
		})
	}

	fun output(message: String, target: String = "STDOUT") {
		addChange(OutputChange().apply {
			this.message = message.eval()
			this.target = target.eval()
		})
	}

	fun setColumnRemarks(tableName: String, columnName: String, catalogName: String? = null, schemaName: String? = null,
						 remarks: String? = null) {
		addChange(SetColumnRemarksChange().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.remarks = remarks?.eval()
		})
	}

	fun setTableRemarks(tableName: String, catalogName: String? = null, schemaName: String? = null, remarks: String? = null) {
		addChange(SetTableRemarksChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.remarks = remarks?.eval()
		})
	}

	fun stop(message: String? = null) {
		addChange(StopChange().apply {
			message?.let {
				this.message = it.eval()
			}
		})
	}

	fun tagDatabase(tag: String) {
		addChange(TagDatabaseChange().apply {
			this.tag = tag.eval()
		})
	}

	fun createProcedure(comments: String? = null, catalogName: String? = null, schemaName: String? = null,
						procedureName: String? = null, dbms: String? = null, path: String? = null,
						relativeToChangelogFile: Any? = null, encoding: String? = null,
						replaceIfExists: Any? = null, procedureText: () -> String) {
		addChange(CreateProcedureChange().apply {
			this.comments = comments?.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.procedureName = procedureName?.eval()
			this.procedureText = procedureText().eval()
			this.dbms = dbms?.eval()
			this.path = path?.eval()
			this.isRelativeToChangelogFile = relativeToChangelogFile?.evalBool()
			this.encoding = encoding?.eval()
			this.replaceIfExists = replaceIfExists?.evalBool()
		})
	}


	fun createTable(tableName: String, catalogName: String? = null, schemaName: String? = null, tablespace: String? = null,
					remarks: String? = null, columns: (KotlinColumn<ColumnConfig>).() -> Unit) {
		addChange(CreateTableChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tablespace = tablespace?.eval()
			this.remarks = remarks?.eval()

			columns.let {
				KotlinColumn(databaseChangeLog, {ColumnConfig()}).apply {
					columns()
				}.columns.forEach(this::addColumn)
			}
		})
	}

	fun createView(viewName: String, catalogName: String? = null, schemaName: String? = null, replaceIfExists: Any? = null,
				   fullDefinition: Any? = null, selectQuery: () -> String) {
		addChange(CreateViewChange().apply {
			this.viewName = viewName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.replaceIfExists = replaceIfExists?.evalBool()
			this.fullDefinition = fullDefinition?.evalBool()
			this.selectQuery = selectQuery()
		})
	}

	fun dropProcedure(procedureName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(DropProcedureChange().apply {
			this.procedureName = procedureName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun dropTable(tableName: String, catalogName: String? = null, schemaName: String? = null, cascadeConstraints: Any? = null) {
		addChange(DropTableChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.isCascadeConstraints = cascadeConstraints?.evalBool()
		})
	}

	fun dropView(viewName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(DropViewChange().apply {
			this.viewName = viewName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun mergeColumns(tableName: String, column1Name: String, column2Name: String, finalColumnName: String, finalColumnType: String,
					 joinString: String? = null, schemaName: String? = null, catalogName: String? = null) {
		addChange(MergeColumnChange().apply {
			this.tableName = tableName.eval()
			this.column1Name = column1Name.eval()
			this.column2Name = column2Name.eval()
			this.finalColumnName = finalColumnName.eval()
			this.finalColumnType = finalColumnType.eval()
			this.joinString = joinString?.eval()
			this.schemaName = schemaName?.eval()
			this.catalogName = catalogName?.eval()
		})
	}

	fun modifyDataType(tableName: String, columnName: String, newDataType: String, catalogName: String? = null,
					   schemaName: String? = null) {
		addChange(ModifyDataTypeChange().apply {
			this.tableName = tableName.eval()
			this.columnName = columnName.eval()
			this.newDataType = newDataType.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun renameColumn(tableName: String, oldColumnName: String, newColumnName: String, catalogName: String? = null,
					 schemaName: String? = null, columnDataType: String? = null, remarks: String? = null) {
		addChange(RenameColumnChange().apply {
			this.tableName = tableName.eval()
			this.oldColumnName = oldColumnName.eval()
			this.newColumnName = newColumnName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.columnDataType = columnDataType?.eval()
			this.remarks = remarks?.eval()
		})
	}

	fun renameView(oldViewName: String, newViewName: String, catalogName: String? = null, schemaName: String? = null) {
		addChange(RenameViewChange().apply {
			this.oldViewName = oldViewName.eval()
			this.newViewName = newViewName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
		})
	}

	fun addForeignKeyConstraint(constraintName: String, baseTableName: String, baseColumnNames: String,
								referencedTableName: String, referencedColumnNames: String,
								baseTableCatalogName: String? = null, baseTableSchemaName: String? = null,
								referencedTableCatalogName: String? = null, referencedTableSchemaName: String? = null,
								deferrable: Any? = null, initiallyDeferred: Any? = null, onUpdate: String? = null,
								onDelete: String? = null) {
		addChange(AddForeignKeyConstraintChange().apply {
			this.constraintName = constraintName.eval()
			this.baseTableName = baseTableName.eval()
			this.baseColumnNames = baseColumnNames.eval()
			this.referencedTableName = referencedTableName.eval()
			this.referencedColumnNames = referencedColumnNames.eval()
			this.baseTableCatalogName = baseTableCatalogName?.eval()
			this.baseTableSchemaName = baseTableSchemaName?.eval()
			this.referencedTableCatalogName = referencedTableCatalogName?.eval()
			this.referencedTableSchemaName = referencedTableSchemaName?.eval()
			this.deferrable = deferrable?.evalBool()
			this.initiallyDeferred = initiallyDeferred?.evalBool()
			this.onUpdate = onUpdate?.eval()
			this.onDelete = onDelete?.eval()
		})
	}

	fun addPrimaryKey(tableName: String, columnNames: String, constraintName: String? = null, catalogName: String? = null,
					  schemaName: String? = null, tablespace: String? = null, clustered: Any? = null, 
					  forIndexName: String? = null, forIndexSchemaName: String? = null, forIndexCatalogName: String? = null) {
		addChange(AddPrimaryKeyChange().apply {
			this.tableName = tableName.eval()
			this.columnNames = columnNames.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.tablespace = tablespace?.eval()
			this.constraintName = constraintName?.eval()
			this.clustered = clustered?.evalBool()
			this.forIndexName = forIndexName?.eval()
			this.forIndexSchemaName = forIndexSchemaName?.eval()
			this.forIndexCatalogName = forIndexCatalogName?.eval()
		})
	}

	fun dropAllForeignKeyConstraints(baseTableName: String, baseTableCatalogName: String? = null,
									 baseTableSchemaName: String? = null) {
		addChange(DropAllForeignKeyConstraintsChange().apply {
			this.baseTableName = baseTableName.eval()
			this.baseTableCatalogName = baseTableCatalogName?.eval()
			this.baseTableSchemaName = baseTableSchemaName?.eval()
		})
	}

	fun dropForeignKeyConstraint(baseTableName: String, constraintName: String, baseTableCatalogName: String? = null,
								 baseTableSchemaName: String? = null) {
		addChange(DropForeignKeyConstraintChange().apply {
			this.baseTableName = baseTableName.eval()
			this.constraintName = constraintName.eval()
			this.baseTableCatalogName = baseTableCatalogName?.eval()
			this.baseTableSchemaName = baseTableSchemaName?.eval()
		})
	}

	fun dropPrimaryKey(tableName: String, catalogName: String? = null, schemaName: String? = null,
					   constraintName: String? = null) {
		addChange(DropPrimaryKeyChange().apply {
			this.tableName = tableName.eval()
			this.catalogName = catalogName?.eval()
			this.schemaName = schemaName?.eval()
			this.constraintName = constraintName?.eval()
		})
	}

	private fun addChange(change: AbstractChange) {
		change.resourceAccessor = resourceAccessor
		changeSet.addChange(change)
	}

	operator fun String.unaryMinus() {
		addChange(RawSQLChange(this))
	}
}