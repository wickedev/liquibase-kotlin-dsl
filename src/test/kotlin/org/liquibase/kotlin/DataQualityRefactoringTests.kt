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

import liquibase.change.core.*
import liquibase.statement.DatabaseFunction
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.*

/**
 * This is one of several classes that test the creation of refactoring changes
 * for ChangeSets. This particular class tests changes that deal with data
 * quality.
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
 * @author Tim Berglund
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class DataQualityRefactoringTests : ChangeSetTests() {

	/**
	 * Test the addAutoIncrement changeSet with no attributes to make sure the
	 * DSL doesn't try to set any defaults.
	 */
	@Test
	fun addAutoIncrementEmpty() {
		buildChangeSet {
			addAutoIncrement(tableName = "", columnName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddAutoIncrementChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertNull(it.columnDataType)
			assertNull(it.startWith)
			assertNull(it.incrementBy)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test the addAutoIncrement change set.
	 */
	@Test
	fun addAutoIncrementFull() {
		buildChangeSet {
			addAutoIncrement(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "angry",
					columnDataType = "boolean",
					startWith = 10,
					incrementBy = 5
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddAutoIncrementChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("angry", it.columnName)
			assertEquals("boolean", it.columnDataType)
			assertEquals(10L, it.startWith.toLong())
			assertEquals(5L, it.incrementBy.toLong())
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Validate the creation of an addDefaultValue change when there are no
	 * attributes set.  Make sure the DSL didn't make up values.
	 */
	@Test
	fun addDefaultValueEmpty() {
		buildChangeSet {
			addDefaultValue(tableName = "", columnName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddDefaultValueChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertNull(it.columnDataType)
			assertNull(it.defaultValue)
			assertNull(it.defaultValueBoolean)
			// it's an Object, so it can be null
			assertNull(it.defaultValueComputed)
			assertNull(it.defaultValueDate)
			assertNull(it.defaultValueNumeric)
			assertNull(it.defaultValueSequenceNext)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test the creation of an addDefaultValue change when all attributes are set.
	 * Remember, the DSL doesn't do any validation - Liquibase does.  We only
	 * care that the DSL sets the proper values in the Liquibase object from the
	 * attribute map.
	 */
	@Test
	fun addDefaultValueFull() {
		buildChangeSet {
			addDefaultValue(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "strength",
					columnDataType = "int",
					defaultValue = "extremely",
					defaultValueBoolean = true,
					defaultValueComputed = "max",
					defaultValueDate = "20101109T130400Z",
					defaultValueNumeric = "2.718281828459045",
					defaultValueSequenceNext = "sequence"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddDefaultValueChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("strength", it.columnName)
			assertEquals("int", it.columnDataType)
			assertEquals("extremely", it.defaultValue)
			assertTrue(it.defaultValueBoolean)
			assertEquals(DatabaseFunction("max"), it.defaultValueComputed)
			assertEquals("20101109T130400Z", it.defaultValueDate)
			assertEquals("2.718281828459045", it.defaultValueNumeric)
			assertEquals("sequence", it.defaultValueSequenceNext.value)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Parse an addLookupTable change with no attributes to make sure the DSL
	 * doesn't make up any defaults.
	 */
	@Test
	fun addLookupTableEmpty() {
		buildChangeSet {
			addLookupTable(existingColumnName = "", existingTableName = "", newColumnName = "", newTableName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddLookupTableChange>(changes[0]) {
			assertNull(it.existingTableCatalogName)
			assertNull(it.existingTableSchemaName)
			assertEquals("", it.existingTableName)
			assertEquals("", it.existingColumnName)
			assertNull(it.newTableCatalogName)
			assertNull(it.newTableSchemaName)
			assertEquals("", it.newTableName)
			assertEquals("", it.newColumnName)
			assertNull(it.newColumnDataType)
			assertNull(it.constraintName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Parse an addLookupTable change with all supported attributes set.
	 */
	@Test
	fun addLookupTableFull() {
		buildChangeSet {
			addLookupTable(
					existingTableCatalogName = "old_catalog",
					existingTableSchemaName = "old_schema",
					existingTableName = "monkey",
					existingColumnName = "emotion",
					newTableCatalogName = "new_catalog",
					newTableSchemaName = "new_schema",
					newTableName = "monkey_emotion",
					newColumnName = "emotion_display",
					newColumnDataType = "varchar(50)",
					constraintName = "fk_monkey_emotion"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddLookupTableChange>(changes[0]) {
			assertEquals("old_catalog", it.existingTableCatalogName)
			assertEquals("old_schema", it.existingTableSchemaName)
			assertEquals("monkey", it.existingTableName)
			assertEquals("emotion", it.existingColumnName)
			assertEquals("new_catalog", it.newTableCatalogName)
			assertEquals("new_schema", it.newTableSchemaName)
			assertEquals("monkey_emotion", it.newTableName)
			assertEquals("emotion_display", it.newColumnName)
			assertEquals("varchar(50)", it.newColumnDataType)
			assertEquals("fk_monkey_emotion", it.constraintName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Parse an addNotNullConstraint with no attributes to make sure the DSL
	 * doesn't make up any defaults.
	 */
	@Test
	fun addNotNullConstraintEmpty() {
		buildChangeSet {
			addNotNullConstraint(tableName = "", columnName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddNotNullConstraintChange>(changes[0]) {
			assertNull(it.constraintName)
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertNull(it.defaultNullValue)
			assertNull(it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Parse an addNotNullConstraint with all supported options set.
	 */
	@Test
	fun addNotNullConstraintFull() {
		buildChangeSet {
			addNotNullConstraint(
					constraintName = "monkey_emotion_nn",
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "emotion",
					defaultNullValue = "angry",
					columnDataType = "varchar(75)"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddNotNullConstraintChange>(changes[0]) {
			assertEquals("monkey_emotion_nn", it.constraintName)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertEquals("angry", it.defaultNullValue)
			assertEquals("varchar(75)", it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an addUniqueConstraint change with no attributes to make sure
	 * the DSL doesn't create any default values.
	 */
	@Test
	fun addUniqueConstraintEmpty() {
		buildChangeSet {
			addUniqueConstraint(tableName = "", columnNames = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddUniqueConstraintChange>(changes[0]) {
			assertNull(it.tablespace)
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnNames)
			assertNull(it.constraintName)
			assertNull(it.deferrable)
			assertNull(it.initiallyDeferred)
			assertNull(it.disabled)
			assertNull(it.forIndexCatalogName)
			assertNull(it.forIndexSchemaName)
			assertNull(it.forIndexName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an addUniqueConstraint change when we have all supported
	 * options.  There are 3 booleans here, so to isolate the attributes, this
	 * test will only set deferrable to true.
	 */
	@Test
	fun addUniqueConstraintFullDeferrable() {
		buildChangeSet {
			addUniqueConstraint(
					tablespace = "tablespace",
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnNames = "species, emotion",
					constraintName = "unique_constraint",
					deferrable = true,
					initiallyDeferred = false,
					disabled = false,
					forIndexCatalogName = "index_catalog",
					forIndexSchemaName = "index_schema",
					forIndexName = "unique_constraint_idx"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddUniqueConstraintChange>(changes[0]) {
			assertEquals("tablespace", it.tablespace)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("species, emotion", it.columnNames)
			assertEquals("unique_constraint", it.constraintName)
			assertTrue(it.deferrable)
			assertFalse(it.initiallyDeferred)
			assertFalse(it.disabled)
			assertEquals("index_catalog", it.forIndexCatalogName)
			assertEquals("index_schema", it.forIndexSchemaName)
			assertEquals("unique_constraint_idx", it.forIndexName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an addUniqueConstraint change when we have all supported
	 * options.  There are 3 booleans here, so to isolate the attributes, this
	 * test will only set initiallyDeferred to true.
	 */
	@Test
	fun addUniqueConstraintFullDeferred() {
		buildChangeSet {
			addUniqueConstraint(
					tablespace = "tablespace",
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnNames = "species, emotion",
					constraintName = "unique_constraint",
					deferrable = false,
					initiallyDeferred = true,
					disabled = false,
					forIndexCatalogName = "index_catalog",
					forIndexSchemaName = "index_schema",
					forIndexName = "unique_constraint_idx"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddUniqueConstraintChange>(changes[0]) {
			assertEquals("tablespace", it.tablespace)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("species, emotion", it.columnNames)
			assertEquals("unique_constraint", it.constraintName)
			assertFalse(it.deferrable)
			assertTrue(it.initiallyDeferred)
			assertFalse(it.disabled)
			assertEquals("index_catalog", it.forIndexCatalogName)
			assertEquals("index_schema", it.forIndexSchemaName)
			assertEquals("unique_constraint_idx", it.forIndexName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an addUniqueConstraint change when we have all supported
	 * options.  There are 3 booleans here, so to isolate the attributes, this
	 * test will only set deferrable to true.
	 */
	@Test
	fun addUniqueConstraintFullDisabled() {
		buildChangeSet {
			addUniqueConstraint(
					tablespace = "tablespace",
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnNames = "species, emotion",
					constraintName = "unique_constraint",
					deferrable = false,
					initiallyDeferred = false,
					disabled = true,
					forIndexCatalogName = "index_catalog",
					forIndexSchemaName = "index_schema",
					forIndexName = "unique_constraint_idx"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AddUniqueConstraintChange>(changes[0]) {
			assertEquals("tablespace", it.tablespace)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("species, emotion", it.columnNames)
			assertEquals("unique_constraint", it.constraintName)
			assertFalse(it.deferrable)
			assertFalse(it.initiallyDeferred)
			assertTrue(it.disabled)
			assertEquals("index_catalog", it.forIndexCatalogName)
			assertEquals("index_schema", it.forIndexSchemaName)
			assertEquals("unique_constraint_idx", it.forIndexName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an alterSequence change with no attributes to make sure the
	 * DSL doesn't create default values
	 */
	@Test
	fun alterSequenceEmpty() {
		buildChangeSet {
			alterSequence(sequenceName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AlterSequenceChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.sequenceName)
			assertNull(it.incrementBy)
			assertNull(it.minValue)
			assertNull(it.maxValue)
			assertNull(it.isOrdered) // it is an Object and can be null.
			assertNull(it.cacheSize)
			assertNull(it.willCycle)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an alterSequence change with all supported attributes
	 * present.
	 */
	@Test
	fun alterSequenceFull() {
		buildChangeSet {
			alterSequence(
					catalogName = "catalog",
					schemaName = "schema",
					sequenceName = "seq",
					incrementBy = 314,
					minValue = 300,
					maxValue = 400,
					ordered = true,
					cacheSize = 10,
					willCycle = true
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<AlterSequenceChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("seq", it.sequenceName)
			assertEquals(314L, it.incrementBy.toLong())
			assertEquals(300L, it.minValue.toLong())
			assertEquals(400L, it.maxValue.toLong())
			assertTrue(it.isOrdered)
			assertEquals(10L, it.cacheSize.toLong())
			assertTrue(it.willCycle)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createSequence change with no attributes to make sure the
	 * DSL doesn't create any defaults.
	 */
	@Test
	fun createSequenceEmpty() {
		buildChangeSet {
			createSequence(sequenceName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateSequenceChange>(changes[0]) {
			assertEquals("", it.sequenceName)
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertNull(it.startValue)
			assertNull(it.incrementBy)
			assertNull(it.minValue)
			assertNull(it.maxValue)
			assertNull(it.isOrdered)
			assertNull(it.cycle)
			assertNull(it.cacheSize)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a createSequence change with all attributes present to make
	 * sure they all go to the right place.
	 */
	@Test
	fun createSequenceFull() {
		buildChangeSet {
			createSequence(
					catalogName = "catalog",
					sequenceName = "sequence",
					schemaName = "schema",
					startValue = 8,
					incrementBy = 42,
					minValue = 7,
					maxValue = 602324,
					ordered = true,
					cacheSize = 314,
					cycle = false
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CreateSequenceChange>(changes[0]) {
			assertEquals("sequence", it.sequenceName)
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals(42L, it.incrementBy.toLong())
			assertEquals(7L, it.minValue.toLong())
			assertEquals(602324, it.maxValue.toLong())
			assertEquals(8L, it.startValue.toLong())
			assertTrue(it.isOrdered)
			assertEquals(314L, it.cacheSize.toLong())
			assertFalse(it.cycle)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropDefaultValue change with no attributes to make sure the
	 * DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	fun dropDefaultValueEmpty() {
		buildChangeSet {
			dropDefaultValue(tableName = "", columnName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropDefaultValueChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertNull(it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropDefaultValue change with all supported attributes
	 */
	@Test
	fun dropDefaultValueFull() {
		buildChangeSet {
			dropDefaultValue(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "emotion",
					columnDataType = "varchar"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropDefaultValueChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertEquals("varchar", it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropNotNullConstraint change with no attributes to make sure
	 * the DSL doesn't introduce unexpected defaults.
	 */
	@Test
	fun dropNotNullConstraintEmpty() {
		buildChangeSet {
			dropNotNullConstraint(tableName = "", columnName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropNotNullConstraintChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.columnName)
			assertNull(it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropNotNullConstraint with all supported attributes.
	 */
	@Test
	fun dropNotNullConstraintFull() {
		buildChangeSet {
			dropNotNullConstraint(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "monkey",
					columnName = "emotion",
					columnDataType = "varchar(75)")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropNotNullConstraintChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
			assertEquals("varchar(75)", it.columnDataType)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropSequence change with no attributes to make sure the DSL
	 * doesn't introduce unexpected defaults.
	 */
	@Test
	fun dropSequenceEmpty() {
		buildChangeSet {
			dropSequence(sequenceName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropSequenceChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.sequenceName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropSequence change with all supported attributes.
	 */
	@Test
	fun dropSequenceFull() {
		buildChangeSet {
			dropSequence(
					catalogName = "catalog",
					schemaName = "schema",
					sequenceName = "sequence"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropSequenceChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("sequence", it.sequenceName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropUniqueConstraint change with no attributes to make sure
	 * the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	fun dropUniqueConstraintEmpty() {
		buildChangeSet {
			dropUniqueConstraint(tableName = "", constraintName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropUniqueConstraintChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.tableName)
			assertEquals("", it.constraintName)
			assertNull(it.uniqueColumns)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a dropUniqueConstraint change with all supported options
	 */
	@Test
	fun dropUniqueConstraintFull() {
		buildChangeSet {
			dropUniqueConstraint(
					catalogName = "catalog",
					schemaName = "schema",
					tableName = "table",
					constraintName = "unique_constraint",
					uniqueColumns = "unique_column"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<DropUniqueConstraintChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("table", it.tableName)
			assertEquals("unique_constraint", it.constraintName)
			assertEquals("unique_column", it.uniqueColumns)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a renameSequence change with no attributes to make sure the
	 * DSL doesn't create any defaults.
	 */
	@Test
	fun renameSequenceEmpty() {
		buildChangeSet {
			renameSequence(oldSequenceName = "", newSequenceName = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RenameSequenceChange>(changes[0]) {
			assertNull(it.catalogName)
			assertNull(it.schemaName)
			assertEquals("", it.oldSequenceName)
			assertEquals("", it.newSequenceName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a renameSequence change with all attributes present to make
	 * sure they all go to the right place.
	 */
	@Test
	fun renameSequenceFull() {
		buildChangeSet {
			renameSequence(
					catalogName = "catalog",
					schemaName = "schema",
					oldSequenceName = "old_sequence",
					newSequenceName = "new_sequence"
			)
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RenameSequenceChange>(changes[0]) {
			assertEquals("catalog", it.catalogName)
			assertEquals("schema", it.schemaName)
			assertEquals("old_sequence", it.oldSequenceName)
			assertEquals("new_sequence", it.newSequenceName)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}
}

