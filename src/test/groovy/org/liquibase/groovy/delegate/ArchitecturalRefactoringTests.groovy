/*
 * Copyright 2011-2014 Tim Berglund and Steven C. Saliman
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

import liquibase.exception.ParseException
import org.junit.Test
import static org.junit.Assert.*
import liquibase.item.core.Column
import liquibase.action.core.CreateIndexesAction
import liquibase.action.core.DropIndexesAction

/**
 * This is one of several classes that test the creation of refactoring actions
 * of ChangeSets. This particular class tests actions that deal with indexes.
 * <p>
 * Since the Groovy DSL parser is meant to act as a pass-through for Liquibase
 * itself, it doesn't do much in the way of error checking.  For example, we
 * aren't concerned with whether or not required attributes are present - we
 * leave that to Liquibase itself.  In general, each action will have 3 kinds
 * of tests:<br>
 * <ol>
 * <li>A test with an empty parameter map, and if supported, an empty closure.
 * This kind of test will make sure that the Groovy parser doesn't introduce
 * any unintended attribute defaults for an action.</li>
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
 * Some actions require a little more testing, such as the {@code sql} action
 * that can receive sql as a string, or as a closure, or the {@code delete}
 * action, which is valid both with and without a child closure.
 * <p>
 * We don't worry about testing combinations that don't make sense, such as
 * allowing a createIndex action with a closure, but no attributes, since it 
 * doesn't make sense to have this kind of action without both a table name and
 * at least one column.  If a user tries it, they will get errors from Liquibase
 * itself.
 *
 * @author Steven C. Saliman
 */
class ArchitecturalRefactoringTests extends IntegrationTest {

	/**
	 * Test parsing a createIndex action with no attributes and an empty
	 * closure.
	 */
	@Test
	void createIndexEmpty() {
		def action = parseAction("""
			createIndex([:]) {}
		""")

		assertTrue action instanceof CreateIndexesAction
		def index = action.indexes[0]
		assertNull index.catalogName
		assertNull index.schemaName
		assertNull index.tableName
		assertNull index.tablespace
		assertNull index.indexName
		assertNull index.unique
		assertNull index.clustered
		assertNull index.associatedWith
		def columns = index.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a createIndex action with all attributes set and one column.
	 * We don't really care too much about the particulars of the column, since
	 * column parsing is tested in the ColumnDelegate tests.
	 */
	@Test
	void createIndexFullOneColumn() {
		def action = parseAction("""
			createIndex(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					tablespace: 'tablespace',
					indexName: 'ndx_monkeys',
					unique: true,
					clustered: false,
					associatedWith: 'foreignKey') {
				column(name: 'name')
			}
		""")

		assertTrue action instanceof CreateIndexesAction
		def index = action.indexes[0]
		assertEquals 'catalog', index.catalogName
		assertEquals 'schema', index.schemaName
		assertEquals 'monkey', index.tableName
		assertEquals 'tablespace', index.tablespace
		assertEquals 'ndx_monkeys', index.indexName
		assertEquals 'foreignKey', index.associatedWith
		assertTrue index.unique
		assertFalse index.clustered
		def columns = index.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof Column }
		assertEquals 1, columns.size()
		assertEquals 'name', columns[0].name
		assertNoOutput()
	}

	/**
	 * Test parsing a createIndex action with more than one column to make sure
	 * we get them both.  This test also swaps the values of the booleans.
	 */
	@Test
	void createIndexMultipleColumns() {
		def action = parseAction("""
			createIndex(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					tablespace: 'tablespace',
					indexName: 'ndx_monkeys',
					unique: false,
					clustered: true,
					associatedWith: 'foreignKey') {
				column(name: 'species')
				column(name: 'name')
			}
		""")

		assertTrue action instanceof CreateIndexesAction
		def index = action.indexes[0]
		assertEquals 'catalog', index.catalogName
		assertEquals 'schema', index.schemaName
		assertEquals 'monkey', index.tableName
		assertEquals 'tablespace', index.tablespace
		assertEquals 'ndx_monkeys', index.indexName
		assertEquals 'foreignKey', index.associatedWith
		assertFalse index.unique
		assertTrue index.clustered
		def columns = index.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof Column }
		assertEquals 2, columns.size()
		assertEquals 'species', columns[0].name
		assertEquals 'name', columns[1].name
		assertNoOutput()
	}

	/**
	 * The createIndex action can take columns, but a where clause is not valid.
	 * Test parsing a createIndex action with a where clause to make sure it gets
	 * rejected.
	 */
	@Test(expected = ParseException)
	void createIndexWithWhereClause() {
		def action = parseAction("""
			createIndex(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					tablespace: 'tablespace',
					indexName: 'ndx_monkeys',
					unique: true,
					clustered: false,
					associatedWith: 'foreignKey') {
				where "it doesn't matter"
			}
		""")
	}

	/**
	 * Test parsing a dropIndex action with no attributes to make sure the DSL
	 * doesn't introduce unexpected defaults.
	 */
	@Test
	void dropIndexEmpty() {
		def action = parseAction("""
			dropIndex([:])
		""")

		assertTrue action instanceof DropIndexesAction
		assertNotNull action.indexes
		assertEquals 0, action.indexes.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropIndex action with all supported attributes.
	 */
	@Test
	void dropIndexFull() {
		def action = parseAction("""
			dropIndex(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					indexName: 'ndx_monkeys',
					associatedWith: 'foreignKey'
			)
		""")

		assertTrue action instanceof DropIndexesAction
		assertNotNull action.indexes
		assertEquals 1, action.indexes.size()
		def index = action.indexes[0]
		assertEquals 'catalog.schema.monkey', index.container.toString()
		assertEquals 'ndx_monkeys', index.name
//		assertEquals 'foreignKey', index.associatedWith
		assertNoOutput()
	}
}

