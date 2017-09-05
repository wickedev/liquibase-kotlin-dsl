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

import liquibase.change.core.*
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * This is one of several classes that test the creation of refactoring changes
 * for ChangeSets. This particular class tests changes that deal with
 * referential integrity.
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
 *  @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ReferentialIntegrityRefactoringTests : ChangeSetTests() {
	/**
	 * Make an addForeignKeyConstraint with all attributes set to make sure the
	 * right values go to the right places.  This also tests proper handling of
	 * the RESTRICT and CASCADE values for the foreign key type.
	 */
	@Test
	fun addForeignKeyConstraintFull() {
		buildChangeSet {
			addForeignKeyConstraint(
					constraintName = "fk_monkey_emotion",
					baseTableCatalogName = "base_catalog",
					baseTableSchemaName = "base_schema",
					baseColumnNames = "emotion_id",
					baseTableName = "monkey",
					referencedTableCatalogName = "referenced_catalog",
					referencedTableSchemaName = "referenced_schema",
					referencedTableName = "emotions",
					referencedColumnNames = "id",
					deferrable = true,
					initiallyDeferred = false,
					onDelete = "RESTRICT",
					onUpdate = "CASCADE"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddForeignKeyConstraintChange>(changes[0]) {
			assertEquals("fk_monkey_emotion", it.constraintName)
			assertEquals("base_catalog", it.baseTableCatalogName)
			assertEquals("base_schema", it.baseTableSchemaName)
			assertEquals("monkey", it.baseTableName)
			assertEquals("emotion_id", it.baseColumnNames)
			assertEquals("referenced_catalog", it.referencedTableCatalogName)
			assertEquals("referenced_schema", it.referencedTableSchemaName)
			assertEquals("emotions", it.referencedTableName)
			assertEquals("id", it.referencedColumnNames)
			assertTrue(it.deferrable)
			assertFalse(it.initiallyDeferred)
			assertEquals("RESTRICT", it.onDelete)
			assertEquals("CASCADE", it.onUpdate)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Liquibase has deprecated, though still documented, attribute
	 * {@code referencedUniqueColumn}, which is currently ignored by Liquibase,
	 * so let's make sure we get a deprecation warning for it.  This test also
	 * validates proper handling of the SET DEFAULT and SET NULL cascade types.
	 */
	@Test
	fun addForeignKeyConstraintWithWithNoActionType() {
		buildChangeSet {
			addForeignKeyConstraint(
					constraintName = "fk_monkey_emotion",
					baseTableCatalogName = "base_catalog",
					baseTableSchemaName = "base_schema",
					baseTableName = "monkey",
					baseColumnNames = "emotion_id",
					referencedTableCatalogName = "referenced_catalog",
					referencedTableSchemaName = "referenced_schema",
					referencedTableName = "emotions",
					referencedColumnNames = "id",
					deferrable = false,
					initiallyDeferred = true,
					onDelete = "NO ACTION",
					onUpdate = "NO ACTION"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddForeignKeyConstraintChange>(changes[0]) {
			assertEquals("fk_monkey_emotion", it.constraintName)
			assertEquals("base_catalog", it.baseTableCatalogName)
			assertEquals("base_schema", it.baseTableSchemaName)
			assertEquals("monkey", it.baseTableName)
			assertEquals("emotion_id", it.baseColumnNames)
			assertEquals("referenced_catalog", it.referencedTableCatalogName)
			assertEquals("referenced_schema", it.referencedTableSchemaName)
			assertEquals("emotions", it.referencedTableName)
			assertEquals("id", it.referencedColumnNames)
			assertFalse(it.deferrable)
			assertTrue(it.initiallyDeferred)
			assertEquals("NO ACTION", it.onDelete)
			assertEquals("NO ACTION", it.onUpdate)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an addPrimaryKey change with all supported attributes set.
	 */
	@Test
	fun addPrimaryKeyFull() {
		buildChangeSet {
			addPrimaryKey(
					constraintName = "pk_monkey",
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnNames = "id",
					tablespace = "tablespace",
					clustered = true,
					forIndexCatalogName = "index_catalog",
					forIndexSchemaName = "index_schema",
					forIndexName = "pk_monkey_idx"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddPrimaryKeyChange>(changes[0]) {
			assertEquals("pk_monkey", it.constraintName)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("tablespace", it.tablespace)
			assertEquals("id", it.columnNames)
			assertTrue(it.clustered)
			assertEquals("index_catalog", it.forIndexCatalogName)
			assertEquals("index_schema", it.forIndexSchemaName)
			assertEquals("pk_monkey_idx", it.forIndexName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropAllForeignKeyConstraints change with all supported
	 * attributes.
	 */
	@Test
	fun dropAllForeignKeyConstraintsFull() {
		buildChangeSet {
			dropAllForeignKeyConstraints(
					baseTableCatalogName = "catalog",
					baseTableSchemaName = "schema",
					baseTableName = "monkey"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropAllForeignKeyConstraintsChange>(changes[0]) {
			assertEquals("catalog", it.baseTableCatalogName)
			assertEquals("schema", it.baseTableSchemaName)
			assertEquals("monkey", it.baseTableName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropForeignKeyConstraint with all supported options.
	 */
	@Test
	fun dropForeignKeyConstraintFull() {
		buildChangeSet {
			dropForeignKeyConstraint(
					baseTableCatalogName = "catalog",
					baseTableSchemaName = "schema",
					baseTableName = "monkey",
					constraintName = "fk_monkey_emotion"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropForeignKeyConstraintChange>(changes[0]) {
			assertEquals("catalog", it.baseTableCatalogName)
			assertEquals("schema", it.baseTableSchemaName)
			assertEquals("monkey", it.baseTableName)
			assertEquals("fk_monkey_emotion", it.constraintName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropPrimaryKey change with all supported attributes.
	 */
	@Test
	fun dropPrimaryKeyFull() {
		buildChangeSet {
			dropPrimaryKey(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					constraintName = "pk_monkey")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropPrimaryKeyChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("pk_monkey", it.constraintName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}
}
