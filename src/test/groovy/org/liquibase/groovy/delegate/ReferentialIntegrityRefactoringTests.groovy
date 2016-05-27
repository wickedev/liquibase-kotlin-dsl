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

package org.liquibase.groovy.delegate

import liquibase.action.core.AddForeignKeysAction
import liquibase.action.core.AddPrimaryKeysAction
import liquibase.action.core.DropAllForeignKeysAction
import liquibase.action.core.DropForeignKeysAction
import liquibase.action.core.DropPrimaryKeysAction
import liquibase.exception.ParseException
import liquibase.item.core.ForeignKey
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/**
 * This is one of several classes that test the creation of refactoring actions
 * for ChangeSets. This particular class tests actions that deal with
 * referential integrity.
 * <p>
 * Since the Groovy DSL parser is meant to act as a pass-through for Liquibase
 * itself, it doesn't do much in the way of error checking.  For example, we
 * aren't concerned with whether or not required attributes are present - we
 * leave that to Liquibase itself.  In general, each action will have 4 kinds
 * of tests:<br>
 * <ol>
 * <li>A test with an empty parameter map, and if supported, an empty closure.
 * This kind of test will make sure that the Groovy parser doesn't introduce
 * any unintended attribute defaults for an action.</li>
 * <li>A test with that is otherwise valid, except for an extra invalid
 * attribute to prove that Liquibase will reject invalid attributes.</li>
 * <li>A test that sets all the attributes known to be supported by Liquibase
 * at this time.  This makes sure that the Groovy parser will send any given
 * groovy attribute to the correct place in Liquibase.  For actions that allow
 * a child closure, this test will include just enough in the closure to make
 * sure it gets processed, and that the right kind of closure is called.</li>
 * <li>Some tests take columns or a where clause in a child closure.  The same
 * closure handles both, but should reject one or the other based on how the
 * closure gets called. These actions will have an additional test with an
 * invalid closure to make sure it sets up the closure properly</li>
 * </ol>
 * <p>
 * Some actions require a little more testing, such as the {@code sql} change
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
 */
class ReferentialIntegrityRefactoringTests extends IntegrationTest {

	/**
	 * Build an addForeignKeyConstraint with no attributes to make sure the DSL
	 * doesn't make up defaults.
	 */
	@Test
	void addForeignKeyConstraintEmpty() {
		def action = parseAction("""
			addForeignKeyConstraint([:])
		""")

		assertTrue action instanceof AddForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 0, action.foreignKeys.size()
		assertNoOutput()
	}

	/**
	 * Try creating an AddForeignKeyConstraint action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void addForeignKeyConstraintInvalid() {
		parseAction("""
			addForeignKeyConstraint(
					constraintName: 'fk_monkey_emotion',
					baseTableCatalogName: 'base_catalog',
					baseTableSchemaName: 'base_schema',
					baseTableName: 'monkey',
					baseColumnNames: 'emotion_id',
					referencedTableCatalogName: 'referenced_catalog',
					referencedTableSchemaName: 'referenced_schema',
					referencedTableName: 'emotions',
					referencedColumnNames: 'id',
					deferrable: true,
					initiallyDeferred: false,
					onDelete: 'RESTRICT',
					onUpdate: 'CASCADE',
					invalidAttribute: 'invalid'
			)
		""")
	}

	/**
	 * Make an addForeignKeyConstraint with all attributes set to make sure the
	 * right values go to the right places.  This also tests proper handling of
	 * the RESTRICT and CASCADE values for the foreign key type.
	 */
	@Test
	void addForeignKeyConstraintFull() {
		def action = parseAction("""
			addForeignKeyConstraint(
					constraintName: 'fk_monkey_emotion',
					baseTableCatalogName: 'base_catalog',
					baseTableSchemaName: 'base_schema',
					baseColumnNames: 'emotion_id',
					baseTableName: 'monkey',
					referencedTableCatalogName: 'referenced_catalog',
					referencedTableSchemaName: 'referenced_schema',
					referencedTableName: 'emotions',
					referencedColumnNames: 'id',
					deferrable: true,
					initiallyDeferred: false,
					onDelete: 'RESTRICT',
					onUpdate: 'CASCADE'
			)
		""")

		assertTrue action instanceof AddForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 1, action.foreignKeys.size()
		def key = action.foreignKeys[0]
		assertEquals 'fk_monkey_emotion', key.name
		assertEquals 'base_catalog.base_schema.monkey', key.relation.toString()
		assertEquals 'referenced_catalog.referenced_schema.emotions', key.referencedTable.toString()
		assertEquals 1, key.columnChecks.size()
		assertEquals 'emotion_id', key.columnChecks[0].baseColumn
		assertEquals 'id', key.columnChecks[0].referencedColumn
		assertTrue key.deferrable
		assertFalse key.initiallyDeferred
		assertEquals ForeignKey.ReferentialAction.restrict, key.deleteRule
		assertEquals ForeignKey.ReferentialAction.cascade, key.updateRule
		assertNoOutput()
	}

	/**
	 * Try adding a foreign key that use the SET DEFAULT and SET NULL cascade
	 * types.
	 */
	@Test
	void addForeignKeyConstraintWithReferencesUniqueColumnProperty() {
		def action = parseAction("""
			addForeignKeyConstraint(
					constraintName: 'fk_monkey_emotion',
					baseTableCatalogName: 'base_catalog',
					baseTableSchemaName: 'base_schema',
					baseTableName: 'monkey',
					baseColumnNames: 'emotion_id',
					referencedTableCatalogName: 'referenced_catalog',
					referencedTableSchemaName: 'referenced_schema',
					referencedTableName: 'emotions',
					referencedColumnNames: 'id',
					deferrable: false,
					initiallyDeferred: true,
					onDelete: 'SET DEFAULT',
					onUpdate: 'SET NULL'
			)
		""")

		assertTrue action instanceof AddForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 1, action.foreignKeys.size()
		def key = action.foreignKeys[0]
		assertEquals 'fk_monkey_emotion', key.name
		assertEquals 'base_catalog.base_schema.monkey', key.relation.toString()
		assertEquals 'referenced_catalog.referenced_schema.emotions', key.referencedTable.toString()
		assertEquals 1, key.columnChecks.size()
		assertEquals 'emotion_id', key.columnChecks[0].baseColumn
		assertEquals 'id', key.columnChecks[0].referencedColumn
		assertFalse key.deferrable
		assertTrue key.initiallyDeferred
		assertEquals ForeignKey.ReferentialAction.setDefault, key.deleteRule
		assertEquals ForeignKey.ReferentialAction.setNull, key.updateRule
		assertNoOutput()
	}

	/**
	 * Make sure we can properly handle the boolean {@code deleteCascade}
	 * attribute to set the CASCADE action on the deleteRule.  Also make sure
	 * we can handle the NO ACTION cascade type.
	 */
	@Test
	void addForeignKeyConstraintWithWithBooleanCascade() {
		def action = parseAction("""
			addForeignKeyConstraint(
					constraintName: 'fk_monkey_emotion',
			        baseTableCatalogName: 'base_catalog',
			        baseTableSchemaName: 'base_schema',
			        baseTableName: 'monkey',
			        baseColumnNames: 'emotion_id',
			        referencedTableCatalogName: 'referenced_catalog',
			        referencedTableSchemaName: 'referenced_schema',
			        referencedTableName: 'emotions',
			        referencedColumnNames: 'id',
			        deferrable: false,
			        initiallyDeferred: true,
			        deleteCascade: true,
			        onUpdate: 'NO ACTION'
			)
		""")

		assertTrue action instanceof AddForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 1, action.foreignKeys.size()
		def key = action.foreignKeys[0]
		assertEquals 'fk_monkey_emotion', key.name
		assertEquals 'base_catalog.base_schema.monkey', key.relation.toString()
		assertEquals 'referenced_catalog.referenced_schema.emotions', key.referencedTable.toString()
		assertEquals 1, key.columnChecks.size()
		assertEquals 'emotion_id', key.columnChecks[0].baseColumn
		assertEquals 'id', key.columnChecks[0].referencedColumn
		assertFalse key.deferrable
		assertTrue key.initiallyDeferred
		assertEquals ForeignKey.ReferentialAction.cascade, key.deleteRule // Set by the deleteCascade:true attribute.
		assertEquals ForeignKey.ReferentialAction.noAction, key.updateRule
		assertNoOutput()
	}

	/**
	 * Test parsing an addPrimaryKey action with no attributes to make sure the
	 * DSL doesn't make up any defaults.
	 */
	@Test
	void addPrimaryKeyEmpty() {
		def action = parseAction("""
			addPrimaryKey([:])
		""")

		assertTrue action instanceof AddPrimaryKeysAction
		assertNotNull action.primaryKeys
		assertEquals 0, action.primaryKeys.size()
		assertNoOutput()
	}

	/**
	 * Test parsing an addPrimaryKey action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void addPrimaryKeyInvalid() {
		parseAction("""
			addPrimaryKey(
					constraintName: 'pk_monkey',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					columnNames: 'id',
					tablespace: 'tablespace',
					invalidAttr: 'invalid'
			)
		""")
	}

	/**
	 * Test parsing an addPrimaryKey action with all supported attributes set.
	 */
	@Test
	void addPrimaryKeyFull() {
		def action = parseAction("""
			addPrimaryKey(
					constraintName: 'pk_monkey',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					columnNames: 'id',
					tablespace: 'tablespace',
//					clustered: true,
//					forIndexCatalogName: 'index_catalog',
//					forIndexSchemaName: 'index_schema',
//					forIndexName: 'pk_monkey_idx'
			)
		""")

		assertTrue action instanceof AddPrimaryKeysAction
		assertNotNull action.primaryKeys
		assertEquals 1, action.primaryKeys.size()
		def key = action.primaryKeys[0]
		assertEquals 'pk_monkey', key.name
		assertEquals 'catalog.schema.monkey', key.relation.toString()
		assertEquals 'tablespace', key.tablespace
		assertEquals 1, key.columns.size()
		assertEquals 'id', key.columns[0].name
//		assertTrue key.clustered
//		assertEquals 'index_catalog', key.forIndexCatalogName
//		assertEquals 'index_schema', key.forIndexSchemaName
//		assertEquals 'pk_monkey_idx', key.forIndexName
		assertNoOutput()
	}

	/**
	 * Test parsing a dropAllForeignKeyConstraints action with no attributes to
	 * make sure the DSL doesn't introduce any defaults..
	 */
	@Test
	void dropAllForeignKeyConstraintsEmpty() {
		def action = parseAction("""
			dropAllForeignKeyConstraints([:])
		""")

		assertTrue action instanceof DropAllForeignKeysAction
		assertNull action.table
		assertNoOutput()
	}

	/**
	 * Test parsing a dropAllForeignKeyConstraints action with an invalid
	 * attribute.
	 */
	@Test(expected = ParseException)
	void dropAllForeignKeyConstraintsInvalid() {
		parseAction("""
			dropAllForeignKeyConstraints(
					baseTableCatalogName: 'catalog',
					baseTableSchemaName: 'schema',
					baseTableName: 'monkey',
					invalidAttr: 'invalid'
			)
		""")
	}

	/**
	 * Test parsing a dropAllForeignKeyConstraints action with all supported
	 * attributes.
	 */
	@Test
	void dropAllForeignKeyConstraintsFull() {
		def action = parseAction("""
			dropAllForeignKeyConstraints(
					baseTableCatalogName: 'catalog',
					baseTableSchemaName: 'schema',
					baseTableName: 'monkey'
			)
		""")

		assertTrue action instanceof DropAllForeignKeysAction
		assertEquals 'catalog.schema.monkey', action.table.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropForeignKeyConstraint action with no attributes to make
	 * sure the DSL doesn't introduce unexpected defaults.
	 */
	@Test
	void dropForeignKeyConstraintEmpty() {
		def action = parseAction("""
			dropForeignKeyConstraint([:])
		""")

		assertTrue action instanceof DropForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 0, action.foreignKeys.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropForeignKeyConstraint with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropForeignKeyConstraintInvalid() {
		parseAction("""
			dropForeignKeyConstraint(
					baseTableCatalogName: 'catalog',
					baseTableSchemaName: 'schema',
					baseTableName: 'monkey',
					constraintName: 'fk_monkey_emotion',
					invalidAttr: 'invalid'
			)
		""")
	}

	/**
	 * Test parsing a dropForeignKeyConstraint with all supported options.
	 */
	@Test
	void dropForeignKeyConstraintFull() {
		def action = parseAction("""
			dropForeignKeyConstraint(
					baseTableCatalogName: 'catalog',
					baseTableSchemaName: 'schema',
					baseTableName: 'monkey',
					constraintName: 'fk_monkey_emotion'
			)
		""")

		assertTrue action instanceof DropForeignKeysAction
		assertNotNull action.foreignKeys
		assertEquals 1, action.foreignKeys.size()
		def key = action.foreignKeys[0]
		assertEquals 'catalog.schema.monkey', key.container.toString()
		assertEquals 'fk_monkey_emotion', key.name
		assertNoOutput()
	}

	/**
	 * Test parsing a dropPrimaryKey action with no attributes to make sure the
	 * DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	void dropPrimaryKeyEmpty() {
		def action = parseAction("""
			dropPrimaryKey([:])
		""")

		assertTrue action instanceof DropPrimaryKeysAction
		assertNotNull action.primaryKeys
		assertEquals 0, action.primaryKeys.size
		assertNoOutput()
	}

	/**
	 * Test parsing a dropPrimaryKey action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropPrimaryKeyInvalid() {
		parseAction("""
			dropPrimaryKey(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					constraintName: 'pk_monkey',
					invalidAttr: 'invalid')
		""")
	}

	/**
	 * Test parsing a dropPrimaryKey action with all supported attributes.
	 */
	@Test
	void dropPrimaryKeyFull() {
		def action = parseAction("""
			dropPrimaryKey(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					constraintName: 'pk_monkey')
		""")

		assertTrue action instanceof DropPrimaryKeysAction
		assertNotNull action.primaryKeys
		assertEquals 1, action.primaryKeys.size
		def key = action.primaryKeys[0]
		assertEquals 'catalog.schema.monkey', key.container.toString()
		assertEquals 'pk_monkey', key.name
		assertNoOutput()
	}
}
