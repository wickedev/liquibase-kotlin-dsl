/*
 * Copyright 2011-2017 Tim Berglund and Steven C. Saliman
 * Kotlin conversion done by Jason Blackwell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.liquibase.kotlin

import liquibase.change.ColumnConfig
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import org.junit.Test
import java.sql.Timestamp
import java.text.SimpleDateFormat
import kotlin.test.*

/**
 * Test class for the {@link ColumnDelegate}.  As usual, we're only verifying
 * that we can pass things to Liquibase correctly. We check all attributes that
 * are known at this time - note that several are undocumented.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ColumnTests {
	private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	private val databaseChangeLog = DatabaseChangeLog().apply {
		changeLogParameters = ChangeLogParameters()
	}

	/**
	 * Build a column with no attributes and no closure to make sure we don't
	 * introduce any unintended defaults.
	 */
	@Test
	fun oneColumnEmptyNoClosure() {
		val delegate = buildColumnDelegate(KotlinColumn(databaseChangeLog, { ColumnConfig() })) {
			column()
		}

		assertEquals(1, delegate.columns.size)
		assertNull(delegate.columns[0].name)
		assertNull(delegate.columns[0].computed)
		assertNull(delegate.columns[0].type)
		assertNull(delegate.columns[0].value)
		assertNull(delegate.columns[0].valueNumeric)
		assertNull(delegate.columns[0].valueBoolean)
		assertNull(delegate.columns[0].valueDate)
		assertNull(delegate.columns[0].valueComputed)
		assertNull(delegate.columns[0].valueSequenceNext)
		assertNull(delegate.columns[0].valueSequenceCurrent)
		assertNull(delegate.columns[0].valueBlobFile)
		assertNull(delegate.columns[0].valueClobFile)
		assertNull(delegate.columns[0].defaultValue)
		assertNull(delegate.columns[0].defaultValueNumeric)
		assertNull(delegate.columns[0].defaultValueDate)
		assertNull(delegate.columns[0].defaultValueBoolean)
		assertNull(delegate.columns[0].defaultValueComputed)
		assertNull(delegate.columns[0].defaultValueSequenceNext)
		assertNull(delegate.columns[0].isAutoIncrement)
		assertNull(delegate.columns[0].startWith)
		assertNull(delegate.columns[0].incrementBy)
		assertNull(delegate.columns[0].remarks)
		assertNull(delegate.columns[0].descending)
		assertNull(delegate.columns[0].constraints)
	}

	/**
	 * Build a column with no attributes and an empty closure to make sure we
	 * don't introduce any unintended defaults.  The main difference between this
	 * and the no closure version is that the presence of a closure will cause
	 * the column to gain constraints with their defaults.
	 */
	@Test
	fun oneColumnEmptyWithClosure() {
		val delegate = buildColumnDelegate(KotlinColumn(databaseChangeLog, { ColumnConfig() })) {
			column {
			}
		}

		assertEquals(1, delegate.columns.size)
		assertNull(delegate.columns[0].name)
		assertNull(delegate.columns[0].computed)
		assertNull(delegate.columns[0].type)
		assertNull(delegate.columns[0].value)
		assertNull(delegate.columns[0].valueNumeric)
		assertNull(delegate.columns[0].valueBoolean)
		assertNull(delegate.columns[0].valueDate)
		assertNull(delegate.columns[0].valueComputed)
		assertNull(delegate.columns[0].valueSequenceNext)
		assertNull(delegate.columns[0].valueSequenceCurrent)
		assertNull(delegate.columns[0].valueBlobFile)
		assertNull(delegate.columns[0].valueClobFile)
		assertNull(delegate.columns[0].defaultValue)
		assertNull(delegate.columns[0].defaultValueNumeric)
		assertNull(delegate.columns[0].defaultValueDate)
		assertNull(delegate.columns[0].defaultValueBoolean)
		assertNull(delegate.columns[0].defaultValueComputed)
		assertNull(delegate.columns[0].defaultValueSequenceNext)
		assertNull(delegate.columns[0].isAutoIncrement)
		assertNull(delegate.columns[0].startWith)
		assertNull(delegate.columns[0].incrementBy)
		assertNull(delegate.columns[0].remarks)
		assertNull(delegate.columns[0].descending)
		assertNotNull(delegate.columns[0].constraints)
	}

	/**
	 * Test creating a column with all currently supported Liquibase attributes.
	 * There are a lot of them, and not all of them are documented. We'd never
	 * use all of them at the same time, but we're only concerned with making
	 * sure any given attribute is properly passed to Liquibase.  Making sure
	 * a change is valid from a Liquibase point of view is between Liquibase and
	 * the change set author.  Note that care was taken to make sure none of the
	 * attribute values match the attribute names.
	 */
	@Test
	fun oneColumnFull() {
		val dateValue = "2010-11-02 07:52:04"
		val columnDateValue = parseSqlTimestamp(dateValue)
		val defaultDate = "2013-12-31 09:30:04"
		val columnDefaultDate = parseSqlTimestamp(defaultDate)

		val delegate = buildColumnDelegate(KotlinColumn(databaseChangeLog, { ColumnConfig() })) {
			column(
					name = "columnName",
					computed = true,
					type = "varchar(30)",
					value = "someValue",
					valueNumeric = 1,
					valueBoolean = false,
					valueDate = dateValue,
					valueComputed = "databaseValue",
					valueSequenceNext = "sequenceNext",
					valueSequenceCurrent = "sequenceCurrent",
					valueBlobFile = "someBlobFile",
					valueClobFile = "someClobFile",
					defaultValue = "someDefaultValue",
					defaultValueNumeric = 2,
					defaultValueDate = defaultDate,
					defaultValueBoolean = false,
					defaultValueComputed = "defaultDatabaseValue",
					defaultValueSequenceNext = "defaultSequence",
					autoIncrement = true, // should be the only true.
					startWith = 3,
					incrementBy = 4,
					remarks = "No comment",
					descending = true
			)
		}

		assertEquals(1, delegate.columns.size)
		assertEquals("columnName", delegate.columns[0].name)
		assertTrue(delegate.columns[0].computed)
		assertEquals("varchar(30)", delegate.columns[0].type)
		assertEquals("someValue", delegate.columns[0].value)
		assertEquals(1, delegate.columns[0].valueNumeric.toInt())
		assertFalse(delegate.columns[0].valueBoolean)
		assertEquals(columnDateValue, delegate.columns[0].valueDate)
		assertEquals("databaseValue", delegate.columns[0].valueComputed.value)
		assertEquals("sequenceNext", delegate.columns[0].valueSequenceNext.value)
		assertEquals("sequenceCurrent", delegate.columns[0].valueSequenceCurrent.value)
		assertEquals("someBlobFile", delegate.columns[0].valueBlobFile)
		assertEquals("someClobFile", delegate.columns[0].valueClobFile)
		assertEquals("someDefaultValue", delegate.columns[0].defaultValue)
		assertEquals(2, delegate.columns[0].defaultValueNumeric.toInt())
		assertEquals(columnDefaultDate, delegate.columns[0].defaultValueDate)
		assertFalse(delegate.columns[0].defaultValueBoolean)
		assertEquals("defaultDatabaseValue", delegate.columns[0].defaultValueComputed.value)
		assertEquals("defaultSequence", delegate.columns[0].defaultValueSequenceNext.value)
		assertTrue(delegate.columns[0].isAutoIncrement)
		assertEquals(3L, delegate.columns[0].startWith.toLong())
		assertEquals(4L, delegate.columns[0].incrementBy.toLong())
		assertEquals("No comment", delegate.columns[0].remarks)
		assertTrue(delegate.columns[0].descending)
		assertNull(delegate.columns[0].constraints)
	}

	/**
	 * Try adding more than one column.  We don't need full columns, we just want
	 * to make sure we can handle more than one column. This will also let us
	 * isolate the booleans a little better.
	 */
	@Test
	fun twoColumns() {
		val delegate = buildColumnDelegate(KotlinColumn(databaseChangeLog, { ColumnConfig() })) {
			// first one has only the boolean value set to true
			column(
					name = "first",
					valueBoolean = true,
					defaultValueBoolean = false,
					autoIncrement = false
			)
			// the second one has just the default value set to true.
			column(
					name = "second",
					valueBoolean = false,
					defaultValueBoolean = true,
					autoIncrement = false
			)
		}

		assertEquals(2, delegate.columns.size)
		assertEquals("first", delegate.columns[0].name)
		assertTrue(delegate.columns[0].valueBoolean)
		assertFalse(delegate.columns[0].defaultValueBoolean)
		assertFalse(delegate.columns[0].isAutoIncrement)
		assertNull(delegate.columns[0].constraints)
		assertEquals("second", delegate.columns[1].name)
		assertFalse(delegate.columns[1].valueBoolean)
		assertTrue(delegate.columns[1].defaultValueBoolean)
		assertFalse(delegate.columns[1].isAutoIncrement)
		assertNull(delegate.columns[1].constraints)
	}

	/**
	 * Try a column that contains a constraint.  We're not concerned with the
	 * contents of the constraint, just that the closure could be called, and the
	 * contents added to the column.
	 */
	@Test
	fun columnWithConstraint() {
		val delegate = buildColumnDelegate(KotlinColumn(databaseChangeLog, { ColumnConfig() })) {
			// first one has only the boolean value set to true
			column(name = "first", type = "int") {
				constraints(nullable = false, unique = true)
			}
		}

		assertEquals(1, delegate.columns.size)
		assertEquals("first", delegate.columns[0].name)
		assertEquals("int", delegate.columns[0].type)
		assertNotNull(delegate.columns[0].constraints)
		assertFalse(delegate.columns[0].constraints.isNullable)
		assertTrue(delegate.columns[0].constraints.isUnique)
	}

	/**
	 * Test creating an "addColumn" column with all currently supported Liquibase
	 * attributes. An "addColumn" column is the same as a normal column, but adds
	 * 3 new attributes for use in an "addColumn" change.  Let's repeat the
	 * {@link #oneColumnFull()} test, but change the type of column to create to
	 * make sure we can set the 3 new attributes.  This is the only "addColumn"
	 * test we'll have since there is not any code in the Delegate itself that
	 * does anything different for "addColumn" columns.  It makes a different type
	 * of object because the caller tells it to.
	 */
	@Test
	fun oneAddColumnFull() {
		val dateValue = "2010-11-02 07:52:04"
		val columnDateValue = parseSqlTimestamp(dateValue)
		val defaultDate = "2013-12-31 09:30:04"
		val columnDefaultDate = parseSqlTimestamp(defaultDate)

		val delegate = buildColumnDelegate(KotlinAddColumn(databaseChangeLog)) {
			column(
					name = "columnName",
					computed = false,
					type = "varchar(30)",
					value = "someValue",
					valueNumeric = 1,
					valueBoolean = false,
					valueDate = dateValue,
					valueComputed = "databaseValue",
					valueSequenceNext = "sequenceNext",
					valueSequenceCurrent = "sequenceCurrent",
					valueBlobFile = "someBlobFile",
					valueClobFile = "someClobFile",
					defaultValue = "someDefaultValue",
					defaultValueNumeric = 2,
					defaultValueDate = defaultDate,
					defaultValueBoolean = false,
					defaultValueComputed = "defaultDatabaseValue",
					defaultValueSequenceNext = "defaultSequence",
					autoIncrement = true, // should be the only true.
					startWith = 3,
					incrementBy = 4,
					remarks = "No comment",
					descending = false,
					beforeColumn = "before",
					afterColumn = "after",
					position = 5
			)
		}

		assertEquals(1, delegate.columns.size)
		assertEquals("columnName", delegate.columns[0].name)
		assertFalse(delegate.columns[0].computed)
		assertEquals("varchar(30)", delegate.columns[0].type)
		assertEquals("someValue", delegate.columns[0].value)
		assertEquals(1, delegate.columns[0].valueNumeric.toInt())
		assertFalse(delegate.columns[0].valueBoolean)
		assertEquals(columnDateValue, delegate.columns[0].valueDate)
		assertEquals("databaseValue", delegate.columns[0].valueComputed.value)
		assertEquals("sequenceNext", delegate.columns[0].valueSequenceNext.value)
		assertEquals("sequenceCurrent", delegate.columns[0].valueSequenceCurrent.value)
		assertEquals("someBlobFile", delegate.columns[0].valueBlobFile)
		assertEquals("someClobFile", delegate.columns[0].valueClobFile)
		assertEquals("someDefaultValue", delegate.columns[0].defaultValue)
		assertEquals(2, delegate.columns[0].defaultValueNumeric.toInt())
		assertEquals(columnDefaultDate, delegate.columns[0].defaultValueDate)
		assertFalse(delegate.columns[0].defaultValueBoolean)
		assertEquals("defaultDatabaseValue", delegate.columns[0].defaultValueComputed.value)
		assertEquals("defaultSequence", delegate.columns[0].defaultValueSequenceNext.value)
		assertTrue(delegate.columns[0].isAutoIncrement)
		assertEquals(3L, delegate.columns[0].startWith.toLong())
		assertEquals(4L, delegate.columns[0].incrementBy.toLong())
		assertEquals("No comment", delegate.columns[0].remarks)
		assertFalse(delegate.columns[0].descending)
		assertEquals("before", delegate.columns[0].beforeColumn)
		assertEquals("after", delegate.columns[0].afterColumn)
		assertEquals(5, delegate.columns[0].position)
		assertNull(delegate.columns[0].constraints)
	}

	/**
	 * Test creating a "loadData" column with all currently supported Liquibase
	 * attributes. A "loadData" column is the same as a normal column, but adds
	 * 2 new attributes.  Let's repeat the {@link #oneColumnFull()} test, but
	 * change the type of column to create to make sure we can set the 2 new
	 * attributes.  This is the only "loadData" test we'll have since there is
	 * not any code in the Delegate itself that does anything different for
	 * "loadData" columns.  It makes a different type of object because the
	 * caller tells it to.
	 */
	@Test
	fun oneLoadDataColumnFull() {
		val dateValue = "2010-11-02 07:52:04"
		val columnDateValue = parseSqlTimestamp(dateValue)
		val defaultDate = "2013-12-31 09:30:04"
		val columnDefaultDate = parseSqlTimestamp(defaultDate)

		val delegate = buildColumnDelegate(KotlinLoadDataColumn(databaseChangeLog)) {
			column(
					name = "columnName",
					computed = true,
					type = "varchar(30)",
					value = "someValue",
					valueNumeric = 1,
					valueBoolean = false,
					valueDate = dateValue,
					valueComputed = "databaseValue",
					valueSequenceNext = "sequenceNext",
					valueSequenceCurrent = "sequenceCurrent",
					valueBlobFile = "someBlobFile",
					valueClobFile = "someClobFile",
					defaultValue = "someDefaultValue",
					defaultValueNumeric = 2,
					defaultValueDate = defaultDate,
					defaultValueBoolean = false,
					defaultValueComputed = "defaultDatabaseValue",
					defaultValueSequenceNext = "defaultSequence",
					autoIncrement = true, // should be the only true.
					startWith = 3,
					incrementBy = 4,
					remarks = "No comment",
					descending = false,
					header = "columnHeader",
					index = 5
			)
		}

		assertEquals(1, delegate.columns.size)
		assertEquals("columnName", delegate.columns[0].name)
		assertTrue(delegate.columns[0].computed)
		assertEquals("varchar(30)", delegate.columns[0].type)
		assertEquals("someValue", delegate.columns[0].value)
		assertEquals(1, delegate.columns[0].valueNumeric.toInt())
		assertFalse(delegate.columns[0].valueBoolean)
		assertEquals(columnDateValue, delegate.columns[0].valueDate)
		assertEquals("databaseValue", delegate.columns[0].valueComputed.value)
		assertEquals("sequenceNext", delegate.columns[0].valueSequenceNext.value)
		assertEquals("sequenceCurrent", delegate.columns[0].valueSequenceCurrent.value)
		assertEquals("someBlobFile", delegate.columns[0].valueBlobFile)
		assertEquals("someClobFile", delegate.columns[0].valueClobFile)
		assertEquals("someDefaultValue", delegate.columns[0].defaultValue)
		assertEquals(2, delegate.columns[0].defaultValueNumeric.toInt())
		assertEquals(columnDefaultDate, delegate.columns[0].defaultValueDate)
		assertFalse(delegate.columns[0].defaultValueBoolean)
		assertEquals("defaultDatabaseValue", delegate.columns[0].defaultValueComputed.value)
		assertEquals("defaultSequence", delegate.columns[0].defaultValueSequenceNext.value)
		assertTrue(delegate.columns[0].isAutoIncrement)
		assertEquals(3L, delegate.columns[0].startWith.toLong())
		assertEquals(4L, delegate.columns[0].incrementBy.toLong())
		assertEquals("No comment", delegate.columns[0].remarks)
		assertFalse(delegate.columns[0].descending)
		assertEquals("columnHeader", delegate.columns[0].header)
		assertEquals(5, delegate.columns[0].index)
		assertNull(delegate.columns[0].constraints)
	}

	/**
	 * Test a column closure that has a where clause.
	 */
	@Test
	fun columnClosureCanContainWhereClause() {
		val delegate = buildColumnDelegate(KotlinUpdateColumn(databaseChangeLog)) {
			column(name = "monkey", type = "VARCHAR(50)")
			where("emotion='angry'")
		}

		assertNotNull(delegate.columns)
		assertEquals(1, delegate.columns.size)
		assertEquals("monkey", delegate.columns[0].name)
		assertEquals("emotion='angry'", delegate.where)
	}

	/**
	 * {@code delete} changes will have a where clause, but no actual columns.
	 * Make sure we can handle this.
	 */
	@Test
	fun columnClosureIsJustWhereClause() {
		val delegate = buildColumnDelegate(KotlinUpdateColumn(databaseChangeLog)) {
			where("emotion='angry'")
		}

		assertNotNull(delegate.columns)
		assertEquals(0, (delegate.columns.size))
		assertEquals("emotion='angry'", (delegate.where))
	}

	/**
	 * helper method to build and execute a ColumnDelegate.
	 * @param closure the closure to execute
	 * @return the new delegate.
	 */
	private fun <T : KotlinColumn<*>> buildColumnDelegate(column: T, closure: (T).() -> Unit): T {
		val changelog = DatabaseChangeLog()
		changelog.changeLogParameters = ChangeLogParameters()
		column.closure()

		return column
	}

	/**
	 * Helper method to parse a string into a date.
	 * @param dateTimeString the string to parse
	 * @return the parsed string
	 */
	private fun parseSqlTimestamp(dateTimeString: String): Timestamp {
		return Timestamp(sdf.parse(dateTimeString).time)
	}
}
