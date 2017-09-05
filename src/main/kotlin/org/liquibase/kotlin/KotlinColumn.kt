package org.liquibase.kotlin

import liquibase.change.ColumnConfig
import liquibase.change.ConstraintsConfig
import liquibase.changelog.DatabaseChangeLog
import java.io.Serializable

open class KotlinColumn<T : ColumnConfig>(
		databaseChangeLog: DatabaseChangeLog,
		private val creator: () -> T) : BaseObject(databaseChangeLog), Serializable {
	internal val columns: MutableList<T> = ArrayList()

	fun column(
			name: String? = null,
			computed: Any? = null,
			type: String? = null,
			value: String? = null,
			valueNumeric: Any? = null,
			valueBoolean: Any? = null,
			valueDate: String? = null,
			valueComputed: String? = null,
			valueSequenceNext: String? = null,
			valueSequenceCurrent: String? = null,
			valueBlobFile: String? = null,
			valueClobFile: String? = null,
			defaultValue: String? = null,
			defaultValueNumeric: Any? = null,
			defaultValueDate: String? = null,
			defaultValueBoolean: Any? = null,
			defaultValueComputed: String? = null,
			defaultValueSequenceNext: String? = null,
			autoIncrement: Any? = null,
			startWith: Any? = null,
			incrementBy: Any? = null,
			remarks: String? = null,
			descending: Any? = null,
			constraint: ((KotlinColumnConstraint).() -> Unit)? = null): T {

		val col = creator().apply {
			this.name = name?.eval()
			this.computed = computed?.evalBool()
			this.type = type?.eval()
			this.value = value?.eval()
			this.valueNumeric = valueNumeric?.evalBigInteger()
			this.valueBoolean = valueBoolean?.evalBool()
			this.setValueDate(valueDate?.eval())
			this.valueComputed = valueComputed?.evalDatabaseFunction()
			this.valueSequenceNext = valueSequenceNext?.evalNextSequence()
			this.valueSequenceCurrent = valueSequenceCurrent?.evalCurrentSequence()
			this.valueBlobFile = valueBlobFile?.eval()
			this.valueClobFile = valueClobFile?.eval()
			this.defaultValue = defaultValue?.eval()
			this.defaultValueNumeric = defaultValueNumeric?.evalBigInteger()
			this.setDefaultValueDate(defaultValueDate?.eval())
			this.defaultValueBoolean = defaultValueBoolean?.evalBool()
			this.defaultValueComputed = defaultValueComputed?.evalDatabaseFunction()
			this.defaultValueSequenceNext = defaultValueSequenceNext?.evalNextSequence()
			this.isAutoIncrement = autoIncrement?.evalBool()
			this.startWith = startWith?.evalBigInteger()
			this.incrementBy = incrementBy?.evalBigInteger()
			this.remarks = remarks?.eval()
			this.descending = descending?.evalBool()
		}

		if (constraint != null) {
			val c = KotlinColumnConstraint(databaseChangeLog)
			c.constraint()
			col.constraints = c.constraint
		}

		columns.add(col)

		return col
	}
}

class KotlinColumnConstraint(databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog) {
	val constraint: ConstraintsConfig = ConstraintsConfig()

	fun constraints(nullable: Any? = null, primaryKey: Any? = null, primaryKeyName: String? = null,
					primaryKeyTablespace: String? = null, unique: Any? = null, uniqueConstraintName: String? = null,
					references: String? = null, referencedTableName: String? = null, referencedColumnNames: String? = null,
					foreignKeyName: String? = null, checkConstraint: String? = null, deleteCascade: Any? = null,
					deferrable: Any? = null, initiallyDeferred: Any? = null) {
		constraint.apply {
			if (nullable != null) {
				this.isNullable = nullable.evalBool()
			}
			if (primaryKey != null) {
				this.isPrimaryKey = primaryKey.evalBool()
			}
			if (primaryKeyName != null) {
				this.primaryKeyName = primaryKeyName.eval()
			}
			if (primaryKeyTablespace != null) {
				this.primaryKeyTablespace = primaryKeyTablespace.eval()
			}
			if (unique != null) {
				this.isUnique = unique.evalBool()
			}
			if (uniqueConstraintName != null) {
				this.uniqueConstraintName = uniqueConstraintName.eval()
			}
			if (references != null) {
				this.references = references.eval()
			}
			if (referencedTableName != null) {
				this.referencedTableName = referencedTableName.eval()
			}
			if (referencedColumnNames != null) {
				this.referencedColumnNames = referencedColumnNames.eval()
			}
			if (foreignKeyName != null) {
				this.foreignKeyName = foreignKeyName.eval()
			}
			if (checkConstraint != null) {
				this.checkConstraint = checkConstraint.eval()
			}
			if (deleteCascade != null) {
				this.isDeleteCascade = deleteCascade.evalBool()
			}
			if (deferrable != null) {
				this.isDeferrable = deferrable.evalBool()
			}
			if (initiallyDeferred != null) {
				this.isInitiallyDeferred = initiallyDeferred.evalBool()
			}
		}
	}
}