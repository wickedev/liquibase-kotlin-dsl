/*
 * Copyright 2011-2017 Tim Berglund and Steven C. Saliman
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
 * for ChangeSets. This particular class tests changes that alter table
 * structure.
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
class StructuralRefactoringTests : ChangeSetTests() {

	/**
	 * Try creating an addColumn change with no attributes and an empty closure.
	 * Make sure the DSL doesn't try to make any assumptions.  It also validates
	 * our assumption that a Liquibase AddColumnChange always has a collection
	 * of columns, even if they are empty.
	 */
	@Test
	fun addColumnEmpty() {
		buildChangeSet {
			addColumn(tableName = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddColumnChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test adding a column with a full set of attributes, and only one column,
	 * which does not have any constraints.  We don't worry about the contents
	 * of the column itself, as we do that when we test the ColumnDelegate.
	 */
	@Test
	fun addColumnFull() {
		buildChangeSet {
			addColumn(catalogName = "zoo", schemaName = "animal", tableName = "monkey") {
				column(name = "monkey_status", type = "varchar(98)")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddColumnChange>(changes[0]) {
			assertEquals("zoo", it.catalogName)
			assertEquals("animal", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(1, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test adding a column with a full set of attributes, and two columns. We
	 * don't worry about the contents of the column, and we won't worry about
	 * columns with constraints, because that will be checked in the tests for
	 * the ColumnDelegate.
	 */
	@Test
	fun addColumnFullWithTwoColumns() {
		buildChangeSet {
			addColumn(catalogName = "zoo", schemaName = "animal", tableName = "monkey") {
				column(name = "monkey_status", type = "varchar(98)")
				column(name = "monkey_business", type = "varchar(98)")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddColumnChange>(changes[0]) {
			assertEquals("zoo", it.catalogName)
			assertEquals("animal", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(2, columns.size)
			assertEquals("monkey_status", columns[0].name)
			assertEquals("monkey_business", columns[1].name)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createProcedure change when we have an empty map and
	 * closure to make sure the DSL doesn't try to set any defaults.
	 */
	@Test
	fun createProcedureEmpty() {
		buildChangeSet {
			createProcedure {""}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateProcedureChange>(changes[0]) {
			assertNull(it.comments)
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertNull(it.procedureName)
			assertEquals("", it.procedureText)
			assertNull(it.dbms)
			assertNull(it.path)
			assertNull(it.isRelativeToChangelogFile)
			assertNull(it.encoding)
			assertNull(it.replaceIfExists)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * test parsing a createProcedure change when we have no attributes
	 * just the body in a closure.  Since the only supported attribute is for
	 * comments, this will be common.
	 */
	@Test
	fun createProcedureClosureOnly() {
		val sql = """\
CREATE OR REPLACE PROCEDURE testMonkey
IS
BEGIN
 -- do something with the monkey
END;"""
		buildChangeSet {
			createProcedure { sql }
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateProcedureChange>(changes[0]) {
			assertNull(it.comments)
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertNull(it.procedureName)
			assertEquals(sql, it.procedureText)
			assertNull(it.dbms)
			assertNull(it.path)
			assertNull(it.isRelativeToChangelogFile)
			assertNull(it.encoding)
			assertNull(it.replaceIfExists)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createProcedure change when we have both comments
	 * and SQL.
	 */
	@Test
	fun createProcedureFull() {
		val sql = """\
CREATE OR REPLACE PROCEDURE testMonkey
IS
BEGIN
 -- do something with the monkey
END;"""
		buildChangeSet {
			createProcedure(
					comments = "someComments",
					catalogName = "catalog",
					schemaName = "schema",
					procedureName = "procedure",
					dbms = "mysql",
					path = "mypath",
					relativeToChangelogFile = false,
					encoding = "utf8",
					replaceIfExists = true) { sql }
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateProcedureChange>(changes[0]) {
			assertEquals("someComments", it.comments)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("procedure", it.procedureName)
			assertEquals(sql, it.procedureText)
			assertEquals("mysql", it.dbms)
			assertEquals("mypath", it.path)
			assertFalse(it.isRelativeToChangelogFile)
			assertEquals("utf8", it.encoding)
			assertTrue(it.replaceIfExists)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createTable change when we have no attributes and an empty
	 * closure.  This just makes sure the DSL doesn't add any defaults.  We
	 * don't need to support no map or no closure because it makes no sense to
	 * have a createTable without at least a name and one column.
	 */
	@Test
	fun createTableEmpty() {
		buildChangeSet {
			createTable(tableName = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateTableChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertNull(it.tablespace)
			assertEquals("", it.tableName)
			assertNull(it.remarks)
			assertNotNull(it.resourceAccessor)

			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createTable change with all supported attributes and a
	 * couple of columns.
	 */
	@Test
	fun createTableFull() {
		buildChangeSet {
			createTable(
					catalogName = "catalog",
					schemaName = "schema",
					tablespace = "oracle_tablespace",
					tableName = "monkey",
					remarks = "angry") {
				column(name = "status", type = "varchar(100)")
				column(name = "id", type = "int")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateTableChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("oracle_tablespace", it.tablespace)
			assertEquals("monkey", it.tableName)
			assertEquals("angry", it.remarks)
			assertNotNull(it.resourceAccessor)

			val columns = it.columns
			assertNotNull(columns)
			assertEquals(2, columns.size)
			assertType<ColumnConfig>(columns[0]) {
				assertEquals("status", columns[0].name)
				assertEquals("varchar(100)", columns[0].type)
			}
			assertType<ColumnConfig>(columns[1]) {
				assertEquals("id", columns[1].name)
				assertEquals("int", columns[1].type)
			}
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createView change with an empty attribute map and an empty
	 * closure to make sure the DSL doesn't introduce any defaults.
	 */
	@Test
	fun createViewEmpty() {
		buildChangeSet {
			createView(viewName = "") {""}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateViewChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.viewName)
			assertNull(it.replaceIfExists)
			assertNull(it.fullDefinition)
			assertEquals("", it.selectQuery)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createView change with all supported attributes and a
	 * closure.  Since createView changes need to have at least a name and
	 * query, we don't need to test for sql by itself.
	 */
	@Test
	fun createViewFull() {
		buildChangeSet {
			createView(
					catalogName = "catalog",
					schemaName = "schema",
					viewName = "monkey_view",
					replaceIfExists = true,
					fullDefinition = false) {
				"SELECT * FROM monkey WHERE state='angry'"
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateViewChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey_view", it.viewName)
			assertTrue(it.replaceIfExists)
			assertFalse(it.fullDefinition)
			assertEquals("SELECT * FROM monkey WHERE state='angry'", it.selectQuery)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn change with no attributes and and empty closure.
	 * This just makes sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	fun dropColumnEmpty() {
		buildChangeSet {
			dropColumn(tableName = "", columnName = "") { }
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropColumnChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertEquals(0, it.columns.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a delete change when we have all attributes and a column
	 * closure.  This probably wouldn't ever get used, but we will support it.
	 */
	@Test
	fun dropColumnFull() {
		buildChangeSet {
			dropColumn(catalogName = "catalog",
					   schemaName = "schema",
					   tableName = "monkey",
					   columnName = "emotion") {
				column(name = "monkey_status")
				column(name = "monkey_business")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropColumnChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(2, columns.size)
			assertEquals("monkey_status", columns[0].name)
			assertEquals("monkey_business", columns[1].name)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn change without a closure. This is the use case
	 * when we put the column names in an attribute instead of the closure, and
	 * is the original way the dropColumn method was used.
	 */
	@Test
	fun dropColumnNoClosure() {
		buildChangeSet {
			dropColumn(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "emotion"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropColumnChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertNotNull(it.resourceAccessor)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test the dropProcedure changeSet with no attributes to make sure the
	 * DSL doesn't try to set any defaults.
	 */
	@Test
	fun dropProcedureEmpty() {
		buildChangeSet {
			dropProcedure(procedureName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropProcedureChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.procedureName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test the dropProcedure change set.
	 */
	@Test
	fun dropProcedureFull() {
		buildChangeSet {
			dropProcedure(
					catalogName = "catalog",
					schemaName = "schema",
					procedureName = "procedureName"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropProcedureChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("procedureName", it.procedureName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropTable change with no attributes to make sure the DSL
	 * doesn't introduce any unexpected changes.
	 */
	@Test
	fun dropTableEmpty() {
		buildChangeSet {
			dropTable(tableName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropTableChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNull(it.isCascadeConstraints)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropTable change with all supported attributes.
	 */
	@Test
	fun dropTableFull() {
		buildChangeSet {
			dropTable(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "fail_table",
					cascadeConstraints = true
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropTableChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("fail_table", it.tableName)
			assertTrue(it.isCascadeConstraints)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropView change with no attributes to make sure the DSL
	 * doesn't introduce any unexpected defaults.
	 */
	@Test
	fun dropViewEmpty() {
		buildChangeSet {
			dropView(viewName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropViewChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.viewName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropView change with all supported options
	 */
	@Test
	fun dropViewFull() {
		buildChangeSet {
			dropView(
					catalogName = "catalog",
					schemaName = "schema",
					viewName = "fail_view"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropViewChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("fail_view", it.viewName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn change when we have all supported attributes.
	 */
	@Test
	fun mergeColumnsFull() {
		buildChangeSet {
			mergeColumns(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "table",
					column1Name = "first_name",
					column2Name = "last_name",
					finalColumnName = "full_name",
					finalColumnType = "varchar(99)",
					joinString = " "
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<MergeColumnChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("table", it.tableName)
			assertEquals("first_name", it.column1Name)
			assertEquals("last_name", it.column2Name)
			assertEquals("full_name", it.finalColumnName)
			assertEquals("varchar(99)", it.finalColumnType)
			assertEquals(" ", it.joinString)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn change when we have all supported attributes.
	 */
	@Test
	fun modifyDataTypeFull() {
		buildChangeSet {
			modifyDataType(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "table",
					columnName = "first_name",
					newDataType = "varchar(99)"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<ModifyDataTypeChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("table", it.tableName)
			assertEquals("first_name", it.columnName)
			assertEquals("varchar(99)", it.newDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a renameColumn change when we have all supported attributes.
	 */
	@Test
	fun renameColumnFull() {
		buildChangeSet {
			renameColumn(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					oldColumnName = "fail",
					newColumnName = "win",
					columnDataType = "varchar(9001)",
					remarks = "just because"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RenameColumnChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("fail", it.oldColumnName)
			assertEquals("win", it.newColumnName)
			assertEquals("varchar(9001)", it.columnDataType)
			assertEquals("just because", it.remarks)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a renameTable change with all supported attributes.
	 */
	@Test
	fun renameTableFull() {
		buildChangeSet {
			renameTable(
					catalogName = "catalog",
					schemaName = "schema",
					oldTableName = "fail_table",
					newTableName = "win_table"
			)
		}

		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RenameTableChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("fail_table", it.oldTableName)
			assertEquals("win_table", it.newTableName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a renameView change with all the supported attributes.
	 */
	@Test
	fun renameViewFull() {
		buildChangeSet {
			renameView(
					catalogName = "catalog",
					schemaName = "schema",
					oldViewName = "fail_view",
					newViewName = "win_view"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RenameViewChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("fail_view", it.oldViewName)
			assertEquals("win_view", it.newViewName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}
}

