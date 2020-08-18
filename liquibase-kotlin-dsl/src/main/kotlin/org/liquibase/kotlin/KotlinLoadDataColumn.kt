package org.liquibase.kotlin

import liquibase.change.core.LoadDataColumnConfig
import liquibase.changelog.DatabaseChangeLog

class KotlinLoadDataColumn(databaseChangeLog: DatabaseChangeLog) : KotlinColumn<LoadDataColumnConfig>(databaseChangeLog, { LoadDataColumnConfig() }) {
	fun column(
			name: String,
			computed: Boolean? = null,
			type: String? = null,
			value: String? = null,
			valueNumeric: Int? = null,
			valueBoolean: Boolean? = null,
			valueDate: String? = null,
			valueComputed: String? = null,
			valueSequenceNext: String? = null,
			valueSequenceCurrent: String? = null,
			valueBlobFile: String? = null,
			valueClobFile: String? = null,
			defaultValue: String? = null,
			defaultValueNumeric: Int? = null,
			defaultValueDate: String? = null,
			defaultValueBoolean: Boolean? = null,
			defaultValueComputed: String? = null,
			defaultValueSequenceNext: String? = null,
			autoIncrement: Boolean? = null,
			startWith: Long? = null,
			incrementBy: Long? = null,
			remarks: String? = null,
			descending: Boolean? = null,
			header: String? = null,
			index: Int? = null,
			constraint: ((KotlinColumnConstraint).() -> Unit)? = null) {
		val col = column(name, computed,type, value, valueNumeric, valueBoolean, valueDate, valueComputed, valueSequenceNext,
				valueSequenceCurrent, valueBlobFile, valueClobFile, defaultValue, defaultValueNumeric, defaultValueDate,
				defaultValueBoolean, defaultValueComputed, defaultValueSequenceNext, autoIncrement, startWith, incrementBy,
				remarks, descending, constraint)

		col.header = header
		col.index = index
	}
}