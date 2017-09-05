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
import liquibase.change.core.CreateIndexChange
import liquibase.change.core.DropIndexChange
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.*

/**
 * This is one of several classes that test the creation of refactoring changes
 * for ChangeSets. This particular class tests changes that deal with indexes.
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
class ArchitecturalRefactoringTests : ChangeSetTests() {

	/**
	 * Test parsing a createIndex changeSet with no attributes and an empty
	 * closure.
	 */
	@Test
	fun createIndexEmpty() {
		buildChangeSet {
			createIndex(tableName = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateIndexChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertNull(it.tablespace)
			assertNull(it.indexName)
			assertNull(it.isUnique)
			assertNull(it.clustered)
			assertNull(it.associatedWith)
			val columns = it.columns
			assertNotNull(columns)
			assertEquals(0, columns.size)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createIndex change with all attributes set and one column.
	 * We don't really care too much about the particulars of the column, since
	 * column parsing is tested in the ColumnDelegate tests.
	 */
	@Test
	fun createIndexFullOneColumn() {
		buildChangeSet {
			createIndex(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					tablespace = "tablespace",
					indexName = "ndx_monkeys",
					unique = true,
					clustered = false,
					associatedWith = "foreignKey") {
				column(name = "name")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateIndexChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("tablespace", it.tablespace)
			assertEquals("ndx_monkeys", it.indexName)
			assertEquals("foreignKey", it.associatedWith)
			assertTrue(it.isUnique)
			assertFalse(it.clustered)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { it is ColumnConfig })
			assertEquals(1, columns.size)
			assertEquals("name", columns[0].name)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createIndex change with more than one column to make sure
	 * we get them both.  This test also swaps the values of the booleans.
	 */
	@Test
	fun createIndexMultipleColumns() {
		buildChangeSet {
			createIndex(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					tablespace = "tablespace",
					indexName = "ndx_monkeys",
					unique = false,
					clustered = true,
					associatedWith = "foreignKey") {
				column(name = "species")
				column(name = "name")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateIndexChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("tablespace", it.tablespace)
			assertEquals("ndx_monkeys", it.indexName)
			assertEquals("foreignKey", it.associatedWith)
			assertFalse(it.isUnique)
			assertTrue(it.clustered)
			val columns = it.columns
			assertNotNull(columns)
			assertTrue(columns.all { it is ColumnConfig })
			assertEquals(2, columns.size)
			assertEquals("species", columns[0].name)
			assertEquals("name", columns[1].name)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropIndex change with no attributes to make sure the DSL
	 * doesn't introduce unexpected defaults.
	 */
	@Test
	fun dropIndexEmpty() {
		buildChangeSet {
			dropIndex(indexName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropIndexChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertNull(it.tableName)
			assertEquals("", it.indexName)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropIndex change with all supported attributes.
	 */
	@Test
	fun dropIndexFull() {
		buildChangeSet {
			dropIndex(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					indexName = "ndx_monkeys",
					associatedWith = "foreignKey"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropIndexChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("ndx_monkeys", it.indexName)
			assertEquals("foreignKey", it.associatedWith)
		}
		assertNoOutput()
	}
}

