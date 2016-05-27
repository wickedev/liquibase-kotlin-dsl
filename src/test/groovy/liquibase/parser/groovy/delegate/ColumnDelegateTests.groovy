/*
 * Copyright 2011-2015 Tim Berglund and Steven C. Saliman
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

package liquibase.parser.groovy.delegate

import liquibase.action.core.LoadDataAction
import liquibase.exception.ParseException
import liquibase.item.core.Column
import liquibase.item.function.SequenceCurrentValueFunction
import liquibase.item.function.SequenceNextValueFunction
import org.junit.Test
import static org.junit.Assert.*
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * Test class to handle all the ways columns can appear in a changelog.  As
 * usual, we're only verifying that we can pass things to Liquibase correctly.
 * We check all attributes that are known at this time - note that several are
 * undocumented.
 *
 * @author Steven C. Saliman
 */
class ColumnDelegateTests extends IntegrationTest {
  def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	// TODO: Figure out where to test the "where" clause.
	/**
	 * Build a column with no attributes and no closure to make sure we don't
	 * introduce any unintended defaults.
	 */
	@Test
	void oneColumnEmptyNoClosure() {
		def action = parseAction("""
			createTable(tableName: 'myTable') {
				column([:])
			}
        """)

		assertEquals 1, action.columns.size()
		def column = action.columns[0]
		assertTrue column instanceof Column
		assertNull column.name
		assertNull column.computed
		assertNull column.type
		assertNull column.value
		assertNull column.valueNumeric
		assertNull column.valueBoolean
		assertNull column.valueDate
		assertNull column.valueComputed
		assertNull column.valueSequenceNext
		assertNull column.valueSequenceCurrent
		assertNull column.valueBlobFile
		assertNull column.valueClobFile
		assertNull column.defaultValue
		assertNull column.defaultValueNumeric
		assertNull column.defaultValueDate
		assertNull column.defaultValueBoolean
		assertNull column.defaultValueComputed
		assertNull column.defaultValueSequenceNext
		assertNull column.autoIncrement
		assertNull column.startWith
		assertNull column.incrementBy
		assertNull column.remarks
		assertNull column.descending
		assertNull column.constraints
	}

	/**
	 * Build a column with no attributes and an empty closure to make sure we
	 * don't introduce any unintended defaults.  The main difference between this
	 * and the no closure version is that the presence of a closure will cause
	 * the column to gain constraints with their defaults.
	 */
	@Test
	void oneColumnEmptyWithClosure() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			column([:]) {}
		}

		assertNull delegate.whereClause
		assertEquals 1, delegate.columns.size()
		assertTrue delegate.columns[0] instanceof Column
		assertNull delegate.columns[0].name
		assertNull delegate.columns[0].computed
		assertNull delegate.columns[0].type
		assertNull delegate.columns[0].value
		assertNull delegate.columns[0].valueNumeric
		assertNull delegate.columns[0].valueBoolean
		assertNull delegate.columns[0].valueDate
		assertNull delegate.columns[0].valueComputed
		assertNull delegate.columns[0].valueSequenceNext
		assertNull delegate.columns[0].valueSequenceCurrent
		assertNull delegate.columns[0].valueBlobFile
		assertNull delegate.columns[0].valueClobFile
		assertNull delegate.columns[0].defaultValue
		assertNull delegate.columns[0].defaultValueNumeric
		assertNull delegate.columns[0].defaultValueDate
		assertNull delegate.columns[0].defaultValueBoolean
		assertNull delegate.columns[0].defaultValueComputed
		assertNull delegate.columns[0].defaultValueSequenceNext
		assertNull delegate.columns[0].autoIncrement
		assertNull delegate.columns[0].startWith
		assertNull delegate.columns[0].incrementBy
		assertNull delegate.columns[0].remarks
		assertNull delegate.columns[0].descending
		assertNotNull delegate.columns[0].constraints
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
	void oneColumnFull() {
		def dateValue = "2010-11-02 07:52:04"
		def columnDateValue = parseSqlTimestamp(dateValue)
		def defaultDate = "2013-12-31 09:30:04"
		def columnDefaultDate = parseSqlTimestamp(defaultDate)

		def action = parseAction("""
			createTable(tableName: 'myTable') {
				column(
					name: 'columnName',
					computed: true,
					type: 'varchar(30)',
//					value: 'someValue',
//					valueNumeric: 1,
//					valueBoolean: false,
//					valueDate: '${dateValue}',
//					valueComputed: 'databaseValue',
//					valueSequenceNext: 'nextSequence',
//					valueSequenceCurrent: 'currentSequence',
//					valueBlobFile: 'someBlobFile',
//					valueClobFile: 'someClobFile',
					defaultValue: 'someDefaultValue',
//					defaultValueNumeric: 2,
//					defaultValueDate: '${defaultDate}',
//					defaultValueBoolean: false,
//					defaultValueComputed: 'defaultDatabaseValue',
//					defaultValueSequenceCurrent: 'defaultCurrentSequence',
//					defaultValueSequenceNext: 'defaultNextSequence',
					autoIncrement: true, // should be the only true.
					startWith: 3,
					incrementBy: 4,
					remarks: 'No comment'
//					descending: true
				)
			}
		""")

		assertEquals 1, action.columns.size()
		def column = action.columns[0]
		assertTrue column instanceof Column
		assertEquals 'columnName', column.name
		assertTrue column.computed
		assertEquals 'varchar(30)', column.type
		assertEquals 'someValue', column.value
		assertEquals 1, column.valueNumeric.intValue()
		assertFalse column.valueBoolean
		assertEquals columnDateValue, column.valueDate
		assertEquals 'databaseValue', column.valueComputed.value
		assertEquals 'sequenceNext', column.valueSequenceNext.value
		assertEquals 'sequenceCurrent', column.valueSequenceCurrent.value
		assertEquals 'someBlobFile', column.valueBlobFile
		assertEquals 'someClobFile', column.valueClobFile
		assertEquals 'someDefaultValue', column.defaultValue
		assertEquals 2, column.defaultValueNumeric.intValue()
		assertEquals columnDefaultDate, column.defaultValueDate
		assertFalse column.defaultValueBoolean
		assertEquals 'defaultDatabaseValue', column.defaultValueComputed.value
		assertEquals 'defaultSequence', column.defaultValueSequenceNext.value
		assertTrue column.autoIncrement
		assertEquals 3G, column.startWith
		assertEquals 4G, column.incrementBy
		assertEquals 'No comment', column.remarks
		assertTrue column.descending
		assertNull column.constraints
	}

	/**
	 * Try adding more than one column.  We don't need full columns, we just want
	 * to make sure we can handle more than one column. This will also let us
	 * isolate the booleans a little better.
	 */
	@Test
	void twoColumns() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			// first one has only the boolean value set to true
			column(
					name: 'first',
					valueBoolean: true,
			        defaultValueBoolean: false,
			        autoIncrement: false
			)
			// the second one has just the default value set to true.
			column(
					name: 'second',
					valueBoolean: false,
					defaultValueBoolean: true,
					autoIncrement: false
			)
		}

		assertNull delegate.whereClause
		assertEquals 2, delegate.columns.size()
		assertTrue delegate.columns[0] instanceof Column
		assertEquals 'first', delegate.columns[0].name
		assertTrue delegate.columns[0].valueBoolean
		assertFalse delegate.columns[0].defaultValueBoolean
		assertFalse delegate.columns[0].autoIncrement
		assertNull delegate.columns[0].constraints
		assertTrue delegate.columns[1] instanceof Column
		assertEquals 'second', delegate.columns[1].name
		assertFalse delegate.columns[1].valueBoolean
		assertTrue delegate.columns[1].defaultValueBoolean
		assertFalse delegate.columns[1].autoIncrement
		assertNull delegate.columns[1].constraints

	}

	/**
	 * Try a column that contains a constraint.  We're not concerned with the
	 * contents of the constraint, just that the closure could be called, and the
	 * contents added to the column.
	 */
	@Test
	void columnWithConstraint() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			// first one has only the boolean value set to true
			column(name: 'first', type: 'int') {
				constraints(nullable: false, unique: true)
			}
		}

		assertNull delegate.whereClause
		assertEquals 1, delegate.columns.size()
		assertTrue delegate.columns[0] instanceof Column
		assertEquals 'first', delegate.columns[0].name
		assertEquals 'int', delegate.columns[0].type
		assertNotNull delegate.columns[0].constraints
		assertFalse delegate.columns[0].constraints.nullable
		assertTrue delegate.columns[0].constraints.unique
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
	void oneAddColumnFull() {
		def dateValue = "2010-11-02 07:52:04"
		def columnDateValue = parseSqlTimestamp(dateValue)
		def defaultDate = "2013-12-31 09:30:04"
		def columnDefaultDate = parseSqlTimestamp(defaultDate)

		def action = parseAction("""
			addColumn(tableName: 'myTable') {
				column(
					name: 'columnName',
//					computed: false,
					type: 'varchar(30)',
//					value: 'someValue',
//					valueNumeric: 1,
//					valueBoolean: false,
//					valueDate: dateValue,
//					valueComputed: 'databaseValue',
//					valueSequenceNext: 'sequenceNext',
//					valueSequenceCurrent: 'sequenceCurrent',
//					valueBlobFile: 'someBlobFile',
//					valueClobFile: 'someClobFile',
//					defaultValue: 'someDefaultValue',
//					defaultValueNumeric: 2,
//					defaultValueDate: '${defaultDate}',
//					defaultValueBoolean: false,
//					defaultValueComputed: 'defaultDatabaseValue',
//					defaultValueSequenceNext: 'defaultNextSequence',
//					defaultValueSequenceCurrent: 'defaultCurrentSequence',
//					autoIncrement: true, // should be the only true.
//					startWith: 3,
//					incrementBy: 4,
//					remarks: 'No comment',
//					descending: false,
			        beforeColumn: 'before',
//			        afterColumn: 'after',
//			        position: 5
				)
			}
		""")

		assertEquals 1, action.columns.size()
		def column = action.columns[0]
		assertEquals 'columnName', column.name
		assertFalse column.computed
		assertEquals 'varchar(30)', column.type
		assertEquals 'someValue', column.value
		assertEquals 1, column.valueNumeric.intValue()
		assertFalse column.valueBoolean
		assertEquals columnDateValue, column.valueDate
		assertEquals 'databaseValue', column.valueComputed.value
		assertEquals 'sequenceNext', column.valueSequenceNext.value
		assertEquals 'sequenceCurrent', column.valueSequenceCurrent.value
		assertEquals 'someBlobFile', column.valueBlobFile
		assertEquals 'someClobFile', column.valueClobFile
		assertEquals 'someDefaultValue', column.defaultValue
		assertEquals 2, column.defaultValueNumeric.intValue()
		assertEquals columnDefaultDate, column.defaultValueDate
		assertFalse column.defaultValueBoolean
		assertEquals 'defaultDatabaseValue', column.defaultValueComputed.value
		assertEquals 'defaultSequence', column.defaultValueSequenceNext.value
		assertTrue column.autoIncrement
		assertEquals 3G, column.startWith
		assertEquals 4G, column.incrementBy
		assertEquals 'No comment', column.remarks
		assertFalse column.descending
		assertEquals 'before', column.beforeColumn
		assertEquals 'after', column.afterColumn
		assertEquals 5, column.position
		assertNull column.constraints
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
	void oneLoadDataColumnFull() {
		def dateValue = "2010-11-02 07:52:04"
		def columnDateValue = parseSqlTimestamp(dateValue)
		def defaultDate = "2013-12-31 09:30:04"
		def columnDefaultDate = parseSqlTimestamp(defaultDate)

		def action = parseAction("""
			loadData(tableName: 'myTable') {
				column(
					name: 'columnName',
//					computed: true,
					type: 'varchar(30)',
//					value: 'someValue',
//					valueNumeric: 1,
//					valueBoolean: false,
//					valueDate: dateValue,
//					valueComputed: 'databaseValue',
//					valueSequenceNext: 'sequenceNext',
//					valueSequenceCurrent: 'sequenceCurrent',
//					valueBlobFile: 'someBlobFile',
//					valueClobFile: 'someClobFile',
//					defaultValue: 'someDefaultValue',
//					defaultValueNumeric: 2,
//					defaultValueDate: '${defaultDate}',
//					defaultValueBoolean: false,
					defaultValueComputed: 'defaultDatabaseValue',
//					defaultValueSequenceNext: 'defaultSequence',
//					autoIncrement: true, // should be the only true.
//					startWith: 3,
//					incrementBy: 4,
//					remarks: 'No comment',
//					descending: false,
			        header: 'columnHeader',
			        index: 5
				)
			}
		""")

		assertEquals 1, action.columns.size()
		def column = action.columns[0]
		assertTrue x instanceof LoadDataAction.LoadDataColumn
		assertEquals 'columnName', column.name
		assertTrue column.computed
		assertEquals 'varchar(30)', column.type
		assertEquals 'someValue', column.value
		assertEquals 1, column.valueNumeric.intValue()
		assertFalse column.valueBoolean
		assertEquals columnDateValue, column.valueDate
		assertEquals 'databaseValue', column.valueComputed.value
		assertEquals 'sequenceNext', column.valueSequenceNext.value
		assertEquals 'sequenceCurrent', column.valueSequenceCurrent.value
		assertEquals 'someBlobFile', column.valueBlobFile
		assertEquals 'someClobFile', column.valueClobFile
		assertEquals 'someDefaultValue', column.defaultValue
		assertEquals 2, column.defaultValueNumeric.intValue()
		assertEquals columnDefaultDate, column.defaultValueDate
		assertFalse column.defaultValueBoolean
		assertEquals 'defaultDatabaseValue', column.defaultValueComputed.value
		assertEquals 'defaultSequence', column.defaultValueSequenceNext.value
		assertTrue column.autoIncrement
		assertEquals 3G, column.startWith
		assertEquals 4G, column.incrementBy
		assertEquals 'No comment', column.remarks
		assertFalse column.descending
		assertEquals 'columnHeader', column.header
		assertEquals 5, column.index
		assertNull column.constraints
	}

	/**
	 * Test a column closure that has a where clause.
	 */
	@Test
	void columnClosureCanContainWhereClause() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			column(name: 'monkey', type: 'VARCHAR(50)')
			where "emotion='angry'"
		}

		assertNotNull delegate.columns
		assertEquals 1, delegate.columns.size()
		assertTrue delegate.columns[0] instanceof Column
		assertEquals 'monkey', delegate.columns[0].name
		assertEquals "emotion='angry'", delegate.whereClause
	}

	/**
	 * {@code delete} changes will have a where clause, but no actual columns.
	 * Make sure we can handle this.
	 */
	@Test
	void columnClosureIsJustWhereClause() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			where "emotion='angry'"
		}

		assertNotNull delegate.columns
		assertEquals 0, delegate.columns.size()
		assertEquals "emotion='angry'", delegate.whereClause
	}

	/**
	 * Try an invalid method in the closure to make sure we get our
	 * ChangeLogParseException instead of the standard MissingMethodException.
	 */
	@Test(expected = ParseException)
	void invalidMethodInClosure() {
		def delegate = buildColumnDelegate(ColumnConfig.class) {
			table(name: 'monkey')
		}

		assertNotNull delegate.columns
		assertEquals 0, delegate.columns.size()
		assertEquals "emotion='angry'", delegate.whereClause
	}


	/**
	 * Try building a column when it contains an invalid attribute.  Do we
	 * get an ChangeLogParseException, which will have our pretty message?
	 * We try to trick the system by using what is a valid "loadData" column
	 * attribute on a normal ColumnConfig.
	 */
	@Test(expected = ParseException)
	void columnWithInvalidAttribute() {
		buildColumnDelegate(ColumnConfig.class) {
			column(header: 'invalid')
		}
	}
}
