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

import liquibase.action.core.AddColumnsAction
import liquibase.action.core.CreateStoredProceduresAction
import liquibase.action.core.CreateTableAction
import liquibase.action.core.CreateViewsAction
import liquibase.action.core.DropColumnsAction
import liquibase.action.core.DropStoredProceduresAction
import liquibase.action.core.DropTablesAction
import liquibase.action.core.DropViewsAction
import liquibase.action.core.MergeColumnsAction
import liquibase.action.core.ModifyDataTypeAction
import liquibase.action.core.RenameColumnAction
import liquibase.action.core.RenameTableAction
import liquibase.action.core.RenameViewAction
import liquibase.exception.ParseException
import liquibase.item.core.Column
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/**
 * This is one of several classes that test the creation of refactoring actions
 * for ChangeSets. This particular class tests actions that alter table
 * structure.
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
 * <li>A test that contains a change that would be valid, except for an extra,
 * invalid, attribute to prove that Liquibase will reject attributes it doesn't
 * recognize.  We need to make sure that the action is otherwise valid so we get
 * the parse exception because of the invalid attribute, and not missing
 * required attributes.</li>
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
class StructuralRefactoringTests extends IntegrationTest {

	/**
	 * Try creating an addColumn action with no attributes and an empty closure.
	 * Make sure the DSL doesn't try to make any assumptions.  It also validates
	 * our assumption that a Liquibase AddColumnsAction always has a collection
	 * of columns, even if they are empty.
	 */
	@Test
	void addColumnEmpty() {
		def action = parseAction("""
			addColumn([:]) {}
		""")

		assertTrue action instanceof AddColumnsAction
		def columns = action.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test adding a column that is valid except for an extra, invalid
	 * attribute.
	 */
	@Test(expected = ParseException)
	void addColumnInvalid() {
		parseAction("""
			addColumn(invalidAttr: 'invalid',
			          catalogName: 'zoo',
			          schemaName: 'animal',
			          tableName: 'monkey') {
				column(name: 'monkey_status', type: 'varchar(98)')
			}
		""");

		assertTrue action instanceof AddColumnsAction
		def columns = action.columns
		assertNotNull columns
		assertEquals 1, columns.size()
		def column = columns[0]
		assertEquals 'zoo.animal.monkey', column.relation.toString()
		assertEquals 'monkey_status', column.name
		assertEquals 'varchar(98)', column.type.toString()
		assertNoOutput()
	}

	/**
	 * Test adding a column with a full set of attributes, and only one column,
	 * which does not have any constraints.
	 */
	@Test
	void addColumnFull() {
		def action = parseAction("""
			addColumn(catalogName: 'zoo', schemaName: 'animal', tableName: 'monkey') {
				column(name: 'monkey_status', type: 'varchar(98)')
			}
		""");

		assertTrue action instanceof AddColumnsAction
		def columns = action.columns
		assertNotNull columns
		assertEquals 1, columns.size()
		def column = columns[0]
		assertEquals 'zoo.animal.monkey', column.relation.toString()
		assertEquals 'monkey_status', column.name
		assertEquals 'varchar(98)', column.type.toString()
		assertNoOutput()
	}

	/**
	 * Test adding a column with a full set of attributes, and two columns. We
	 * don't worry too much about the contents of the column, and we won't worry
	 * about columns with constraints, because that will be checked in a
	 * separate class just for testing columns.  We'll only worry about the
	 * the parts of the column that comes from the addColumn attributes.
	 */
	@Test
	void addColumnFullWithTwoColumns() {
		def action = parseAction("""
			addColumn(catalogName: 'zoo',
			          schemaName: 'animal',
			          tableName: 'monkey') {
				column(name: 'monkey_status', type: 'varchar(98)')
				column(name: 'monkey_business', type: 'varchar(98)')
			}
		""")

		assertTrue action instanceof AddColumnsAction
		assertNotNull action.columns
		assertEquals 2, action.columns.size()
		def column = action.columns[0]

		assertEquals 'zoo.animal.monkey', column.relation.toString()
		assertEquals 'monkey_status', column.name
		assertEquals 'varchar(98)', column.type.toString()

		column = action.columns[1]
		assertEquals 'zoo.animal.monkey', column.relation.toString()
		assertEquals 'monkey_business', column.name
		assertEquals 'varchar(98)', column.type.toString()
		assertNoOutput()
	}

	/**
	 * A where clause is not valid for addColumn, so try one and make sure it
	 * gets rejected.
	 */
	@Test(expected = ParseException)
	void addColumnWithWhereClause() {
		parseAction("""
			addColumn(catalogName: 'zoo', schemaName: 'animal', tableName: 'monkey') {
				where "invalid"
			}
		""")
	}

	/**
	 * Test parsing a createProcedure action when we have an empty map and
	 * closure to make sure the DSL doesn't try to set any defaults.
	 */
	@Test
	void createProcedureEmpty() {
		def action = parseAction("""
			createProcedure ([:]) {}
		""")

//		assertNull action.comments
		assertNotNull action.procedures
		assertEquals 0, action.procedures.size()
		assertNull action.dbms
		assertNull action.replaceIfExists
		assertNoOutput()
	}

	/**
	 * Test parsing a createProcedure action that would be valid if it wasn't
	 * for the extra invalid attribute.
	 */
	@Test(expected = ParseException)
	void createProcedureInvalid() {
		def sql = """CREATE OR REPLACE PROCEDURE testMonkey IS BEGIN -- do something with the monkey END;"""
		parseAction("""
			createProcedure(
					invalidAttr: 'invalid',
					// comments: 'someComments',
					catalogName: 'catalog',
					schemaName: 'schema',
					procedureName: 'procedure',
					dbms: 'mysql',
					// path: 'mypath',
					relativeToChangelogFile: false,
					// encoding: 'utf8',
					replaceIfExists: true) { \"${sql}\" }
		""")
	}

	/**
	 * test parsing a createProcedure action when we have no attributes
	 * just the body in a closure.  Since the only supported attribute is for
	 * comments, this will be common.
	 */
	@Test
	void createProcedureClosureOnly() {
		def sql = """CREATE OR REPLACE PROCEDURE testMonkey IS BEGIN -- do something with the monkey END;"""
		def action = parseAction("""
			createProcedure { \"${sql}\" }
		""")

		assertTrue action instanceof CreateStoredProceduresAction
		assertNotNull action.procedures
		assertEquals 1, action.procedures.size()
		def procedure = action.procedures[0]
//		assertNull action.comments
		assertNull procedure.container
		assertNull procedure.name
		assertEquals sql, procedure.body
		assertNull action.dbms
//		assertNull action.path
//		assertNull action.relativeToChangelogFile
//		assertNull action.encoding
		assertNull action.replaceIfExists
		assertNoOutput()
	}

	/**
	 * Test parsing a createProcedure action when we have no attributes, just
	 * the procedure body as a string.  Since the only supported attribute is
	 * for comments, this will be common.
	 */
	@Test
	void createProcedureSqlOnlyAsString() {
		def sql = """CREATE OR REPLACE PROCEDURE testMonkey IS BEGIN -- do something with the monkey END;"""
		def action = parseAction("""
			createProcedure \"${sql}\"
		""")

		assertTrue action instanceof CreateStoredProceduresAction
		assertNotNull action.procedures
		assertEquals 1, action.procedures.size()
		def procedure = action.procedures[0]
//		assertNull action.comments
		assertNull procedure.container
		assertNull procedure.name
		assertEquals sql, procedure.body
		assertNull action.dbms
//		assertNull action.path
//		assertNull action.relativeToChangelogFile
//		assertNull action.encoding
		assertNull action.replaceIfExists
		assertNoOutput()
	}

	/**
	 * Test parsing a createProcedure action when we have both comments and SQL.
	 */
	@Test
	void createProcedureFull() {
		def sql = """CREATE OR REPLACE PROCEDURE testMonkey IS BEGIN -- do something with the monkey END;"""
		def action = parseAction("""
			createProcedure(
					// comments: 'someComments',
					catalogName: 'catalog',
					schemaName: 'schema',
					procedureName: 'procedure',
					dbms: 'mysql',
					// path: 'mypath',
					relativeToChangelogFile: false,
					// encoding: 'utf8',
					replaceIfExists: true) { \"${sql}\" }
		""")

		assertTrue action instanceof CreateStoredProceduresAction
		assertNotNull action.procedures
		assertEquals 1, action.procedures.size()
		def procedure = action.procedures[0]
//		assertEquals 'someComments', action.comments
		assertEquals 'catalog.schema', procedure.container.toString()
		assertEquals 'procedure', procedure.name
		assertEquals sql, procedure.body
		assertEquals 'mysql', action.dbms
//		assertEquals 'mypath', action.path
//		assertFalse action.relativeToChangelogFile
//		assertEquals 'utf8', action.encoding
		assertTrue action.replaceIfExists
		assertNoOutput()
	}

	/**
	 * Test parsing a createTable action when we have no attributes and an empty
	 * closure.  This just makes sure the DSL doesn't add any defaults.  We
	 * don't need to support no map or no closure because it makes no sense to
	 * have a createTable without at least a name and one column.
	 */
	@Test
	void createTableEmpty() {
		def action = parseAction("""
			createTable([:]) {}
		""")

		assertTrue action instanceof CreateTableAction
		assertNotNull action.table
		def table = action.table
		assertNull table.schema
		assertNull table.tablespace
		assertNull table.name
		assertNull table.remarks

		def columns = action.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a createTable action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void createTableInvalid() {
		parseAction("""
			createTable(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					tablespace: 'oracle_tablespace',
					tableName: 'monkey',
					remarks: 'angry') {
				column(name: 'status', type: 'varchar(100)')
				column(name: 'id', type: 'int')
			}
		""")
	}

	/**
	 * Test parsing a createTable action with all supported attributes and a
	 * couple of columns.
	 */
	@Test
	void createTableFull() {
		def action = parseAction("""
			createTable(
					catalogName: 'catalog',
					schemaName: 'schema',
					tablespace: 'oracle_tablespace',
					tableName: 'monkey',
					remarks: 'angry') {
				column(name: 'status', type: 'varchar(100)')
				column(name: 'id', type: 'int')
			}
		""")

		assertTrue action instanceof CreateTableAction
		assertNotNull action.table
		def table = action.table
		assertEquals 'catalog', table.schema.container.name
		assertEquals 'schema', table.schema.name
		assertEquals 'oracle_tablespace', table.tablespace
		assertEquals 'monkey', table.name
		assertEquals 'angry', table.remarks

		def columns = action.columns
		assertNotNull columns
		assertEquals 2, columns.size()
		assertTrue columns[0] instanceof Column
		assertEquals 'status', columns[0].name
		assertEquals 'varchar(100)', columns[0].type.toString()
		assertTrue columns[1] instanceof Column
		assertEquals 'id', columns[1].name
		assertEquals 'int', columns[1].type.toString()
		assertNoOutput()
	}

	/**
	 * A where clause is not valid for createTable, so try one and make sure it
	 * gets rejected.
	 */
	@Test(expected = ParseException)
	void createTableWithWhereClause() {
		parseAction("""
			createTable(catalogName: 'zoo', schemaName: 'animal', tableName: 'monkey') {
				where "invalid"
			}
		""")
	}

	/**
	 * Test parsing a createView action with an empty attribute map and an empty
	 * closure to make sure the DSL doesn't introduce any defaults.
	 */
	@Test
	void createViewEmpty() {
		def action = parseAction("""
			createView([:]) {}
		""")

		assertTrue action instanceof CreateViewsAction
		assertNotNull action.views
		assertEquals 0, action.views.size()
		assertNull action.replaceIfExists
		assertNoOutput()
	}

	/**
	 * Test parsing a createView action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void createViewInvalid() {
		def action = parseAction("""
			createView(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					viewName: 'monkey_view',
					replaceIfExists: true,
					fullDefinition: false) {
				"SELECT * FROM monkey WHERE state='angry'"
			}
		""")
	}

	/**
	 * Test parsing a createView action with all supported attributes and a
	 * closure.  Since createView actions need to have at least a name and
	 * query, we don't need to test for sql by itself.
	 */
	@Test
	void createViewFull() {
		def action = parseAction("""
			createView(
					catalogName: 'catalog',
					schemaName: 'schema',
					viewName: 'monkey_view',
					replaceIfExists: true,
					fullDefinition: false) {
				"SELECT * FROM monkey WHERE state='angry'"
			}
		""")

		assertTrue action instanceof CreateViewsAction
		assertNotNull action.views
		assertEquals 1, action.views.size()
		def view = action.views[0]
		assertEquals 'catalog.schema', view.schema.toString()
		assertEquals 'monkey_view', view.name
		assertTrue action.replaceIfExists
		assertFalse view.completeDefinition
		assertEquals "SELECT * FROM monkey WHERE state='angry'", view.definition.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn action with no attributes and and empty closure.
	 * This just makes sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	void dropColumnEmpty() {
		def action = parseAction("""
			dropColumn([:]) { }
		""")

		assertTrue action instanceof DropColumnsAction
		assertNotNull action.columns
		assertEquals 0, action.columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn action when we have an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropColumnInvalid() {
		parseAction("""
			dropColumn(invalidAttr: 'invalid',
					   catalogName: 'catalog',
					   schemaName: 'schema',
					   tableName: 'monkey',
					   columnName: 'emotion') {
				column(name: 'monkey_status')
				column(name: 'monkey_business')
			}
		""")
	}

	/**
	 * Test parsing a dropColumn action when we have all attributes and a column
	 * closure.  This probably wouldn't ever get used, but we will support it.
	 */
	@Test
	void dropColumnFull() {
		def action = parseAction("""
			dropColumn(catalogName: 'catalog',
					   schemaName: 'schema',
					   tableName: 'monkey',
					   columnName: 'emotion') {
				column(name: 'monkey_status')
				column(name: 'monkey_business')
			}
		""")

		assertTrue action instanceof DropColumnsAction
		assertNotNull action.columns
		assertEquals 3, action.columns.size()
		// Liquibase appears to process the child nodes before the attribute
		// column.
		def column = action.columns[0]
		assertEquals 'catalog.schema.monkey', column.container.toString()
		assertEquals 'monkey_status', column.name
		column = action.columns[1]
		assertEquals 'catalog.schema.monkey', column.container.toString()
		assertEquals 'monkey_business', column.name
		column = action.columns[2]
		assertEquals 'catalog.schema.monkey', column.container.toString()
		assertEquals 'emotion', column.name
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn action without a closure. This is the use case
	 * when we put the column names in an attribute instead of the closure, and
	 * is the original way the dropColumn method was used.
	 */
	@Test
	void dropColumnNoClosure() {
		def action = parseAction("""
			dropColumn(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					columnName: 'emotion'
			)
		""")

		assertTrue action instanceof DropColumnsAction
		assertNotNull action.columns
		assertEquals 1, action.columns.size()
		// Liquibase appears to process the child nodes before the attribute
		// column.
		def column = action.columns[0]
		assertEquals 'catalog.schema.monkey', column.container.toString()
		assertEquals 'emotion', column.name
		assertNoOutput()
	}

	/**
	 * Test parsing a dropColumn action when we have an invalid method in the
	 * closure. This is not allowed and should be caught by the parser.
	 */
	@Test(expected = ParseException)
	void dropColumnWithWhere() {
		parseAction("""
			dropColumn(catalogName: 'catalog',
					   schemaName: 'schema',
					   tableName: 'monkey',
					   columnName: 'emotion') {
				where(name: 'emotion')
			}
		""")
	}

	/**
	 * Test the dropProcedure action with no attributes to make sure the
	 * DSL doesn't try to set any defaults.
	 */
	@Test
	void dropProcedureEmpty() {
		def action = parseAction("""
			dropProcedure([:])
		""")

		assertTrue action instanceof DropStoredProceduresAction
		assertNotNull action.procedures
		assertEquals 0, action.procedures.size()
		assertNoOutput()
	}

	/**
	 * Test the dropProcedure action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropProcedureInvalid() {
		parseAction("""
			dropProcedure(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					procedureName: 'procedureName'
			)
		""")
	}

	/**
	 * Test the dropProcedure action with all supported attributes.
	 */
	@Test
	void dropProcedureFull() {
		def action = parseAction("""
			dropProcedure(
					catalogName: 'catalog',
					schemaName: 'schema',
					procedureName: 'procedureName'
			)
		""")

		assertTrue action instanceof DropStoredProceduresAction
		assertNotNull action.procedures
		assertEquals 1, action.procedures.size()
		assertEquals 'catalog.schema', action.procedures[0].container.toString()
		assertEquals 'procedureName', action.procedures[0].name
		assertNoOutput()
	}

	/**
	 * Test parsing a dropTable action with no attributes to make sure the DSL
	 * doesn't introduce any unexpected changes.
	 */
	@Test
	void dropTableEmpty() {
		def action = parseAction("""
			dropTable([:])
		""")

		assertTrue action instanceof DropTablesAction
		assertNotNull action.tables
		assertEquals 0, action.tables.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropTable action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropTableInvalid() {
		parseAction("""
			dropTable(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'fail_table',
					cascadeConstraints: true
			)
		""")
	}

	/**
	 * Test parsing a dropTable action with all supported attributes.
	 */
	@Test
	void dropTableFull() {
		def action = parseAction("""
			dropTable(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'fail_table',
					cascadeConstraints: true
			)
		""")

		assertTrue action instanceof DropTablesAction
		assertNotNull action.tables
		assertEquals 1, action.tables.size()
		assertEquals 'catalog.schema', action.tables[0].container.toString()
		assertEquals 'fail_table', action.tables[0].name
		assertTrue action.cascadeConstraints
		assertNoOutput()
	}

	/**
	 * Test parsing a dropView action with no attributes to make sure the DSL
	 * doesn't introduce any unexpected defaults.
	 */
	@Test
	void dropViewEmpty() {
		def action = parseAction("""
			dropView([:])
		""")

		assertTrue action instanceof DropViewsAction
		assertNotNull action.views
		assertEquals 0, action.views.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a dropView action with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void dropViewInvalid() {
		parseAction("""
			dropView(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					viewName: 'fail_view'
			)
		""")
	}

	/**
	 * Test parsing a dropView action with all supported options
	 */
	@Test
	void dropViewFull() {
		def action = parseAction("""
			dropView(
					catalogName: 'catalog',
					schemaName: 'schema',
					viewName: 'fail_view'
			)
		""")

		assertTrue action instanceof DropViewsAction
		assertNotNull action.views
		assertEquals 1, action.views.size()
		assertEquals 'catalog.schema', action.views[0].container.toString()
		assertEquals 'fail_view', action.views[0].name
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn action when there are no attributes to make
	 * sure the DSL doesn't introduce unintended defaults.
	 */
	@Test
	void mergeColumnsEmpty() {
		def action = parseAction("""
			mergeColumns([:])
		""")

		assertTrue action instanceof MergeColumnsAction
		assertNull action.relation
		assertNull action.column1Name
		assertNull action.column2Name
		assertNull action.finalColumnName
		assertNull action.finalColumnType
		assertNull action.joinString
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn action when we have invalid attributes.
	 */
	@Test(expected = ParseException)
	void mergeColumnsInvalid() {
		parseAction("""
			mergeColumns(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'table',
					column1Name: 'first_name',
					column2Name: 'last_name',
					finalColumnName: 'full_name',
					finalColumnType: 'varchar(99)',
					joinString: ' '
			)
		""")
	}

	/**
	 * Test parsing a mergeColumn action when we have all supported attributes.
	 */
	@Test
	void mergeColumnsFull() {
		def action = parseAction("""
			mergeColumns(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'table',
					column1Name: 'first_name',
					column2Name: 'last_name',
					finalColumnName: 'full_name',
					finalColumnType: 'varchar(99)',
					joinString: ' '
			)
		""")

		assertTrue action instanceof MergeColumnsAction
		assertEquals 'catalog.schema.table', action.relation.toString()
		assertEquals 'first_name', action.column1Name
		assertEquals 'last_name', action.column2Name
		assertEquals 'full_name', action.finalColumnName
		assertEquals 'varchar(99)', action.finalColumnType.toString()
		assertEquals ' ', action.joinString
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn action when there are no attributes to make
	 * sure the DSL doesn't introduce unintended defaults.
	 */
	@Test
	void modifyDataTypeEmpty() {
		def action = parseAction("""
			modifyDataType([:])
		""")

		assertTrue action instanceof ModifyDataTypeAction
		assertNull action.column
		assertNull action.newDataType
		assertNoOutput()
	}

	/**
	 * Test parsing a mergeColumn action when we have invalid attributes.
	 */
	@Test(expected = ParseException)
	void modifyDataTypeInvalid() {
		parseAction("""
			modifyDataType(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'table',
					columnName: 'first_name',
					newDataType: 'varchar(99)'
			)
		""")
	}

	/**
	 * Test parsing a mergeColumn action when we have all supported attributes.
	 */
	@Test
	void modifyDataTypeFull() {
		def action = parseAction("""
			modifyDataType(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'table',
					columnName: 'first_name',
					newDataType: 'varchar(99)'
			)
		""")

		assertTrue action instanceof ModifyDataTypeAction
		assertNotNull action.column
		def column = action.column
		assertEquals 'catalog.schema.table', column.container.toString()
		assertEquals 'first_name', column.name
		assertEquals 'varchar(99)', action.newDataType.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a renameColumn action when we have no attributes to make sure
	 * the DSL doesn't introduce any unintended defaults.
	 */
	@Test
	void renameColumnEmpty() {
		def action = parseAction("""
			renameColumn([:])
		""")

		assertTrue action instanceof RenameColumnAction
		assertNull action.relation
		assertNull action.oldName
		assertNull action.newName
		assertNull action.columnDefinition
		assertNull action.remarks
		assertNoOutput()
	}

	/**
	 * Test parsing a renameColumn action when we have invalid attributes.
	 */
	@Test(expected = ParseException)
	void renameColumnInvalid() {
		parseAction("""
			renameColumn(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					oldColumnName: 'fail',
					newColumnName: 'win',
					columnDataType: 'varchar(9001)',
					remarks: 'just because'
			)
		""")
	}

	/**
	 * Test parsing a renameColumn action when we have all supported attributes.
	 */
	@Test
	void renameColumnFull() {
		def action = parseAction("""
			renameColumn(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					oldColumnName: 'fail',
					newColumnName: 'win',
					columnDataType: 'varchar(9001)',
					remarks: 'just because'
			)
		""")

		assertTrue action instanceof RenameColumnAction
		assertEquals 'catalog.schema.monkey', action.relation.toString()
		assertEquals 'fail', action.oldName
		assertEquals 'win', action.newName
		assertEquals 'varchar(9001)', action.columnDefinition.toString()
		assertEquals 'just because', action.remarks
		assertNoOutput()
	}

	/**
	 * Test parsing a renameTable action when we have no attributes to make sure
	 * we don't get any unintended defaults.
	 */
	@Test
	void renameTableEmpty() {
		def action = parseAction("""
			renameTable([:])
		""")

		assertTrue action instanceof RenameTableAction
		assertNull action.oldName
		assertNull action.newName
		assertNoOutput()
	}

	/**
	 * Test parsing a renameTable action with invalid attributes.
	 */
	@Test(expected = ParseException)
	void renameTableInvalid() {
		parseAction("""
			renameTable(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					oldTableName: 'fail_table',
					newTableName: 'win_table'
			)
		""")
	}

	/**
	 * Test parsing a renameTable action with all supported attributes.
	 */
	@Test
	void renameTableFull() {
		def action = parseAction("""
			renameTable(
					catalogName: 'catalog',
					schemaName: 'schema',
					oldTableName: 'fail_table',
					newTableName: 'win_table'
			)
		""")

		assertTrue action instanceof RenameTableAction
		assertEquals 'catalog.schema', action.oldName.container.toString()
//		assertEquals 'fail_table', action.oldName.name
		assertEquals 'catalog.schema', action.newName.container.toString()
//		assertEquals 'win_table', action.newName.name
		assertNoOutput()
	}

	/**
	 * Test parsing a renameView action when we have no attributes to make sure
	 * we don't get any unintended defaults.
	 */
	@Test
	void renameViewEmpty() {
		def action = parseAction("""
			renameView([:])
		""")

		assertTrue action instanceof RenameViewAction
		assertNull action.oldName
		assertNull action.newName
		assertNoOutput()
	}

	/**
	 * Test parsing a renameView action with invalid attributes.
	 */
	@Test(expected = ParseException)
	void renameViewInvalid() {
		parseAction("""
			renameView(
					invalidAttr: 'invalid',
					catalogName: 'catalog',
					schemaName: 'schema',
					oldViewName: 'fail_view',
					newViewName: 'win_view'
			)
		""")
	}

	/**
	 * Test parsing a renameView action with all the supported attributes.
	 */
	@Test
	void renameViewFull() {
		def action = parseAction("""
			renameView(
					catalogName: 'catalog',
					schemaName: 'schema',
					oldViewName: 'fail_view',
					newViewName: 'win_view'
			)
		""")

		assertTrue action instanceof RenameViewAction
		assertEquals 'catalog.schema', action.oldName.container.toString()
//		assertEquals 'fail_table', action.oldName.name
		assertEquals 'catalog.schema', action.newName.container.toString()
//		assertEquals 'win_table', action.newName.name
		assertNoOutput()
	}

}

