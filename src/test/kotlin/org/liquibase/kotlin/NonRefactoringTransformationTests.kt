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
import liquibase.change.core.*
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.*

/**
 * This is one of several classes that test the creation of refactoring changes
 * for ChangeSets. This particular class tests changes that deal with data, such as
 * {@code insert} and {@code delete}.
 * <p>
 * Since the Kotlin DSL parser is meant to act as a pass-through for Liquibase
 * itself, it doesn't do much in the way of error checking.  For example, we
 * aren't concerned with whether or not required attributes are present - we
 * leave that to Liquibase itself.  In general, each change will have 3 kinds
 * of tests:<br>
 * <ol>
 * <li>A test with an empty parameter map, and if supported, an empty closure.
 * This kind of test will make sure that the Kotlin parser doesn't introduce
 * any unintended attribute defaults for a change.</li>
 * <li>A test that sets all the attributes known to be supported by Liquibase
 * at this time.  This makes sure that the Kotlin parser will send any given
 * kotlin attribute to the correct place in Liquibase.  For changes that allow
 * a child closure, this test will include just enough in the closure to make
 * sure it gets processed, and that the right kind of closure is called.</li>
 * <li>Some tests take columns or a where clause in a child closure.  The same
 * closure handles both, but should reject one or the other based on how the
 * closure gets called. These changes will have an additional test with an
 * invalid closure to make sure it sets up the closure properly</li>
 * </ol>
 * <p>
 * Some changes require a little more testing, such as the {@code sql} change
 * that can receive sql as a string, or as a closure, or the {@code delete}
 * change, which is valid both with and without a child closure.
 * <p>
 * We don't worry about testing combinations that don't make sense, such as
 * allowing a createIndex change a closure, but no attributes, since it doesn't
 * make sense to have this kind of change without both a table name and at
 * least one column.  If a user tries it, they will get errors from Liquibase
 * itself.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class NonRefactoringTransformationTests : ChangeSetTests() {
	/**
	 * Test parsing a delete change with no attributes and no where clause.  This
	 * just makes sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	fun deleteDataEmpty() {
		buildChangeSet {
			delete(tableName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DeleteDataChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNull(it.where)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a delete change when we have all attributes and a where clause
	 */
	@Test
	fun deleteDataFull() {
		buildChangeSet {
			delete(catalogName = "catalog", schemaName = "schema", tableName = "monkey") {
				where("emotion='angry' AND active=true")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DeleteDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion='angry' AND active=true", it.where)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a delete change without a closure. This just means we have
	 * no "where" clause, and should be supported.
	 */
	@Test
	fun deleteDataNoWhereClause() {
		buildChangeSet {
			delete(catalogName = "catalog", schemaName = "schema", tableName = "monkey")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DeleteDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertNull(it.where)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Process the "empty" change.  This doesn't do anything more than verify
	 * that we can have one without blowing up.  Note that
	 */
	@Test
	fun emptyChange() {
		buildChangeSet {
			empty()
		}
	}

	/**
	 * Test parsing an insert change with no attributes and no columns to make
	 * sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	fun insertEmpty() {
		buildChangeSet {
			insert(tableName = "") {}
		}

		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<InsertDataChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNull(it.dbms)
			assertEquals(0, it.columns.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}
	/**
	 * Test parsing an insert when we have all supported attributes and some
	 * columns.  We don't need to worry about columns without attributes or
	 * attributes without columns because those scenarios don't make any sense.
	 */
	@Test
	fun insertFull() {
		val now = "2010-11-02 07:52:04"
		val sqlNow = parseSqlTimestamp(now)
		buildChangeSet {
			insert(catalogName = "catalog",
				   schemaName = "schema",
				   tableName = "monkey",
			       dbms = "oracle, db2") {
				column(name = "id", valueNumeric = 502)
				column(name = "emotion", value = "angry")
				column(name = "last_updated", valueDate = now)
				column(name = "active", valueBoolean = true)
			}
		}

		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<InsertDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("oracle, db2", it.dbms)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { column -> column is ColumnConfig })
			assertEquals(4, columns.size)
			assertEquals("id", columns[0].name)
			assertEquals(502, columns[0].valueNumeric.toInt())
			assertEquals("emotion", columns[1].name)
			assertEquals("angry", columns[1].value)
			assertEquals("last_updated", columns[2].name)
			assertEquals(sqlNow, columns[2].valueDate)
			assertEquals("active", columns[3].name)
			assertTrue(columns[3].valueBoolean)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a loadData change when the attribute map and column closure
	 * are both empty.  We don't need to worry about the map or the closure
	 * being missing because that kind of change doesn't make sense.  In this
	 * case, Liquibase itself has defaults for the separator and quote chars,
	 * which is what we check in the test.
	 */
	@Test
	fun loadDataEmpty() {
		buildChangeSet {
			loadData(tableName = "", file = "") {}
		}

		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<LoadDataChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.file)
			assertNull(it.isRelativeToChangelogFile)
			assertNull(it.encoding)
			assertEquals(",", it.separator)
			assertEquals("\"", it.quotchar)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a loadDataChange with all supported attributes and a few
	 * columns.  We're not too concerned with the column contents, just make
	 * sure we get them, including the extra attributes that are supported for
	 * columns in a loadData change.  For this test, we want a separator and
	 * quotchar that is different from the Liquibase defaults, so we'll go with
	 * semi-colon separated and single quoted
	 */
	@Test
	fun loadDataFull() {
		buildChangeSet {
			loadData(catalogName = "catalog",
					 schemaName = "schema",
					 tableName = "monkey",
					 file = "data.csv",
					 relativeToChangelogFile = true,
					 encoding = "UTF-8",
					 separator = ";",
					 quotchar = "\"") {
				column(name = "id", index = 1, header = "id_header")
				column(name = "emotion", index = 2, header = "emotion_header")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<LoadDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("data.csv", it.file)
			assertTrue(it.isRelativeToChangelogFile)
			assertEquals("UTF-8", it.encoding)
			assertEquals(";", it.separator)
			assertEquals("\"", it.quotchar)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { column -> column is LoadDataColumnConfig })
			assertEquals(2, columns.size)
			assertEquals("id", columns[0].name)
			assertEquals(1, columns[0].index)
			assertEquals("id_header", columns[0].header)
			assertEquals("emotion", columns[1].name)
			assertEquals(2, columns[1].index)
			assertEquals("emotion_header", columns[1].header)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a loadData change when the attribute map and column closure
	 * are both empty.  We don't need to worry about the map or the closure
	 * being missing because that kind of change doesn't make sense.  In this
	 * case, Liquibase itself has defaults for the separator and quote chars,
	 * which is what we check in the test.
	 */
	@Test
	fun loadUpdateDataEmpty() {
		buildChangeSet {
			loadUpdateData(tableName = "", file = "", primaryKey = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<LoadUpdateDataChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.file)
			assertNull(it.isRelativeToChangelogFile)
			assertNull(it.encoding)
			assertEquals(",", it.separator)
			assertEquals("\"", it.quotchar)
			assertEquals("", it.primaryKey)
			assertFalse(it.onlyUpdate) // False is the Lioquibase default
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a loadDataChange with all supported attributes and a few
	 * columns.  We're not too concerned with the column contents, just make sure
	 * we get them.  For this test, we want a separator and quotchar that is
	 * different from the Liquibase defaults, so we'll go with  semi-colon
	 * separated and single quoted
	 */
	@Test
	fun loadUpdateDataFull() {
		buildChangeSet {
			loadUpdateData(catalogName = "catalog",
					       schemaName = "schema",
					       tableName = "monkey",
					       file = "data.csv",
					       relativeToChangelogFile = true,
					       encoding = "UTF-8",
					       separator = ";",
					       quotchar = "'",
			               primaryKey = "id",
			               onlyUpdate = true) {
				column(name = "id")
				column(name = "emotion")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<LoadUpdateDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("data.csv", it.file)
			assertTrue(it.isRelativeToChangelogFile)
			assertEquals("UTF-8", it.encoding)
			assertEquals(";", it.separator)
			assertEquals("'", it.quotchar)
			assertEquals("id", it.primaryKey)
			assertTrue(it.onlyUpdate)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { column -> column is LoadDataColumnConfig })
			assertEquals(2, columns.size)
			assertEquals("id", columns[0].name)
			assertEquals("emotion", columns[1].name)
		}
		assertNoOutput()
	}

	/**
	 * Test an output change with all supported properties
	 */
	@Test
	fun outputFull() {
		buildChangeSet {
			output(message = "some helpful message",
			        target = "STDOUT")
		}

		assertEquals(1, changeSet.changes.size)
		assertType<OutputChange>(changeSet.changes[0]) {
			assertEquals("some helpful message", it.message)
			assertEquals("STDOUT", it.target)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test an output change with all supported properties
	 */
	@Test
	fun setColumnRemarksFull() {
		buildChangeSet {
			setColumnRemarks(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "emotion",
					remarks = "some helpful message"
			)
		}

		assertEquals(1, changeSet.changes.size)
		assertType<SetColumnRemarksChange>(changeSet.changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertEquals("some helpful message", it.remarks)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test an output change with all supported properties
	 */
	@Test
	fun setTableRemarksFull() {
		buildChangeSet {
			setTableRemarks(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					remarks = "some helpful message"
			)
		}

		assertEquals(1, changeSet.changes.size)
		assertType<SetTableRemarksChange>(changeSet.changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("some helpful message", it.remarks)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a stop change with an empty parameter map.  In this case, we
	 * expect Liquibase to give us a default message.
	 */
	@Test
	fun stopEmpty() {
		buildChangeSet {
			stop()
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<StopChange>(changes[0]) {
			assertEquals("Stop command in changelog file", it.message)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a stop change when the message is in the attributes.
	 */
	@Test
	fun stopMessageInAttributes() {
		buildChangeSet {
			stop(message = "Stop the refactoring. Just...stop.")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<StopChange>(changes[0]) {
			assertEquals("Stop the refactoring. Just...stop.", it.message)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a stop change when the message is not in an attribute.
	 */
	@Test
	fun stopMessageIsArgument() {
		buildChangeSet {
			stop("Stop the refactoring. Just...stop.")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<StopChange>(changes[0]) {
			assertEquals("Stop the refactoring. Just...stop.", it.message)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a tagDatabase change when we have all supported attributes.
	 */
	@Test
	fun tagDatabaseNameInAttributes() {
		buildChangeSet {
			tagDatabase(tag = "monkey")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<TagDatabaseChange>(changes[0]) {
			assertEquals("monkey", it.tag)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a tagDatabase change when the name is not in an attribute.
	 */
	@Test
	fun tagDatabaseNameIsArgument() {
		buildChangeSet {
			tagDatabase("monkey")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<TagDatabaseChange>(changes[0]) {
			assertEquals("monkey", it.tag)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * test parsing an updateData change with no attributes and no closure to
	 * make sure the DSL is not adding any unintended defaults.
	 */
	@Test
	fun updateDataEmpty() {
		buildChangeSet {
			update(tableName = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<UpdateDataChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNull(it.where)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an updateData change when we have all supported attributes,
	 * and a couple of columns, but no where clause.  This should not cause an
	 * issue, since it is legal to update all rows in a table. As always, we don't
	 * care about the contents of the columns.
	 */
	@Test
	fun updateDataNoWhere() {
		buildChangeSet {
			update(catalogName = "catalog",  schemaName = "schema", tableName = "monkey") {
				column(name = "rfid_tag")
				column(name = "emotion")
				column(name = "last_updated")
				column(name = "active")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<UpdateDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertNull(it.where)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { column -> column is ColumnConfig })
			assertEquals(4, columns.size)
			assertEquals("rfid_tag", columns[0].name)
			assertEquals("emotion", columns[1].name)
			assertEquals("last_updated", columns[2].name)
			assertEquals("active", columns[3].name)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an updateData change when we have attributes, columns and
	 * a where clause.  We won't test a where and no columns because that change
	 * doesn't make sense, and will be rejected by Liquibase itself.
	 */
	@Test
	fun updateDataFull() {
		buildChangeSet {
			update(catalogName = "catalog",  schemaName = "schema", tableName = "monkey") {
				column(name = "rfid_tag")
				column(name = "emotion")
				column(name = "last_updated")
				column(name = "active")
				where("id=882")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<UpdateDataChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("id=882", it.where)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { column -> column is ColumnConfig })
			assertEquals(4, columns.size)
			assertEquals("rfid_tag", columns[0].name)
			assertEquals("emotion", columns[1].name)
			assertEquals("last_updated", columns[2].name)
			assertEquals("active", columns[3].name)
		}
		assertNoOutput()
	}
}

