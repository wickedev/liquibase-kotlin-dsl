package org.liquibase.kotlin

import liquibase.change.AddColumnConfig
import liquibase.changelog.DatabaseChangeLog

class KotlinAddColumn(databaseChangeLog: DatabaseChangeLog) : KotlinColumn<AddColumnConfig>(databaseChangeLog, { AddColumnConfig() }) {
	fun column(
			name: String,
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
			beforeColumn: String? = null,
			afterColumn: String? = null,
			position: Any? = null,
			constraint: ((KotlinColumnConstraint).() -> Unit)? = null) {
		val col = column(name, computed,type, value, valueNumeric, valueBoolean, valueDate, valueComputed, valueSequenceNext,
				valueSequenceCurrent, valueBlobFile, valueClobFile, defaultValue, defaultValueNumeric, defaultValueDate,
				defaultValueBoolean, defaultValueComputed, defaultValueSequenceNext, autoIncrement, startWith, incrementBy,
				remarks, descending, constraint)

		col.beforeColumn = beforeColumn?.eval()
		col.afterColumn = afterColumn?.eval()
		col.position = position?.evalInt()
	}
}