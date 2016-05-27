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

import liquibase.action.core.AlterRemarksAction
import liquibase.action.core.DeleteDataAction
import liquibase.action.core.InsertDataAction
import liquibase.action.core.LoadDataAction
import liquibase.action.core.OutputMessageAction
import liquibase.action.core.ThrowExceptionAction
import liquibase.action.core.UpdateDataAction
import liquibase.exception.ParseException
import liquibase.item.core.Column
import liquibase.resource.FileSystemResourceAccessor
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/**
 * This is one of several classes that test the creation of refactoring actions
 * for ChangeSets. This particular class tests actions that deal with data,
 * such as {@code insert} and {@code delete}.
 * <p>
 * Since the Groovy DSL parser is meant to act as a pass-through for Liquibase
 * itself, it doesn't do much in the way of error checking.  For example, we
 * aren't concerned with whether or not required attributes are present - we
 * leave that to Liquibase itself.  In general, each change will have 4 kinds
 * of tests:<br>
 * <ol>
 * <li>A test with an empty parameter map, and if supported, an empty closure.
 * This kind of test will make sure that the Groovy parser doesn't introduce
 * any unintended attribute defaults for a change.</li>
 * <li>A test with that is otherwise valid, except for an extra invalid
 * attribute to prove that Liquibase will reject invalid attributes.</li>
 * <li>A test that sets all the attributes known to be supported by Liquibase
 * at this time.  This makes sure that the Groovy parser will send any given
 * groovy attribute to the correct place in Liquibase.  For actions that allow
 * a child closure, this test will include just enough in the closure to make
 * sure it gets processed, and that the right kind of closure is called.</li>
 * <li>Some tests take columns or a where clause in a child closure.  The same
 * closure handles both, but should reject one or the other based on how the
 * closure gets called. These changes will have an additional test with an
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
class NonRefactoringTransformationTests extends IntegrationTest {

	/**
	 * Test parsing a delete action with no attributes and no where clause.
	 * This just makes sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	void deleteDataEmpty() {
		def action = parseAction("""
			delete([:]) { }
		""")

		assertTrue action instanceof DeleteDataAction
		assertNull action.relation
		assertEquals "", action.where.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a delete action when we have an invalid attribute
	 */
	@Test(expected = ParseException)
	void deleteDataInvalid() {
		parseAction("""
			delete(catalogName: 'catalog',
				   schemaName: 'schema',
				   tableName: 'monkey',
				    invalidAttr: 'invalid') {
				where "emotion='angry' AND active=true"
			}
		""")
	}

	/**
	 * Test parsing a delete action when we have all attributes and a where
	 * clause
	 */
	@Test
	void deleteDataFull() {
		def action = parseAction("""
			delete(catalogName: 'catalog', schemaName: 'schema', tableName: 'monkey') {
				where "emotion='angry' AND active=true"
			}
		""")

		assertTrue action instanceof DeleteDataAction
		assertEquals 'catalog.schema.monkey', action.relation.toString()
		assertEquals "emotion='angry' AND active=true", action.where.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a delete action without a closure. This just means we have
	 * no "where" clause, and should be supported.
	 */
	@Test
	void deleteDataNoWhereClause() {
		def action = parseAction("""
			delete(catalogName: 'catalog', schemaName: 'schema', tableName: 'monkey')
		""")

		assertTrue action instanceof DeleteDataAction
		assertEquals 'catalog.schema.monkey', action.relation.toString()
		assertEquals "", action.where.toString()
		assertNoOutput()
	}

	/**
	 * Test parsing a delete action when we have columns in the closure.  This
	 * is not allowed and should be caught by the parser.
	 */
	@Test(expected = ParseException)
	void deleteDataWithColumns() {
		parseAction("""
			delete(catalogName: 'catalog', schemaName: 'schema', tableName: 'monkey') {
				column(name: 'emotion')
			}
		""")
	}

	/**
	 * Process the "empty" action.  This doesn't do anything more than verify
	 * that we can have one without blowing up.  We can't use the regular
	 * {@code parseAction} helper here because there will be no action in the
	 * resulting changeSet - Liquibase just ignores {@code empty} actions.
	 */
	@Test
	void emptyChange() {
		def changeSet = parseChangeSet("""
			changeSet(id: 'myChange', author: 'steve') {
				empty()
			}
		""")
		assertNotNull changeSet
		assertNotNull changeSet.actions
		assertEquals 0, changeSet.actions.size()
	}

	/**
	 * Test parsing an insert action with no attributes and no columns to make
	 * sure the DSL doesn't introduce any unexpected defaults.
	 */
	@Test
	void insertEmpty() {
		def action = parseAction("""
			insert([:]) {	}
		""")

		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof InsertDataAction
		assertNull action.catalogName
		assertNull action.schemaName
		assertNull action.tableName
		assertNull action.dbms
		assertEquals 0, action.columns.size
		assertNoOutput()
	}
	/**
	 * Test parsing an insert when we have all supported attributes and some
	 * columns.  We don't need to worry about columns without attributes or
	 * attributes without columns because those scenarios don't make any sense.
	 */
	@Test
	void insertFull() {
		def now = '2010-11-02 07:52:04'
		def sqlNow = parseSqlTimestamp(now)
		def action = parseAction("""
			insert(catalogName: 'catalog',
				   schemaName: 'schema',
				   tableName: 'monkey',
			       dbms: 'oracle, db2') {
				column(name: 'id', valueNumeric: 502)
				column(name: 'emotion', value: 'angry')
				column(name: 'last_updated', valueDate: now)
				column(name: 'active', valueBoolean: true)
			}
		""")

		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof InsertDataAction
		assertEquals 'catalog', action.catalogName
		assertEquals 'schema', action.schemaName
		assertEquals 'monkey', action.tableName
		assertEquals 'oracle, db2', action.dbms
		def columns = action.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof Column }
		assertEquals 4, columns.size()
		assertEquals 'id', columns[0].name
		assertEquals 502, columns[0].valueNumeric.intValue()
		assertEquals 'emotion', columns[1].name
		assertEquals 'angry', columns[1].value
		assertEquals 'last_updated', columns[2].name
		assertEquals sqlNow, columns[2].valueDate
		assertEquals 'active', columns[3].name
		assertTrue columns[3].valueBoolean
		assertNoOutput()
	}

	/**
	 * The insert action allows columns, but not a where clause.  Try an insert
	 * with a where clause to make sure it is properly rejected.
	 */
	@Test(expected = ParseException)
	void insertWithWhereClause() {
		def action = parseAction("""
			insert(catalogName: 'catalog',
							schemaName: 'schema',
							tableName: 'monkey',
							dbms: 'oracle, db2') {
				where "invalid"
			}
		""")
	}

	/**
	 * Test parsing a loadData action when the attribute map and column closure
	 * are both empty.  We don't need to worry about the map or the closure
	 * being missing because that kind of change doesn't make sense.  In this
	 * case, Liquibase itself has defaults for the separator and quote chars,
	 * which is what we check in the test.
	 */
	@Test
	void loadDataEmpty() {
		resourceAccessor = new FileSystemResourceAccessor()
		def action = parseAction("""
			loadData([:]) {	}
		""")

		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof LoadDataAction
		assertNull action.catalogName
		assertNull action.schemaName
		assertNull action.tableName
		assertNull action.file
		assertNull action.relativeToChangelogFile
		assertNull action.encoding
		assertEquals ",", action.separator
		assertEquals '"', action.quotchar
		assertNotNull 'LoadDataChange.resourceAccessor should not be null', action.resourceAccessor
		def columns = action.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a loadData action with all supported attributes and a few
	 * columns.  We're not too concerned with the column contents, just make
	 * sure we get them, including the extra attributes that are supported for
	 * columns in a loadData action.  For this test, we want a separator and
	 * quotchar that is different from the Liquibase defaults, so we'll go with
	 * semi-colon separated and single quoted
	 */
	@Test
	void loadDataFull() {
		resourceAccessor = new FileSystemResourceAccessor()

		def action = parseAction("""
			loadData(catalogName: 'catalog',
					 schemaName: 'schema',
					 tableName: 'monkey',
					 file: 'data.csv',
					 relativeToChangelogFile: true,
					 encoding: 'UTF-8',
					 separator: ';',
					 quotchar: "'",
                   commentLineStartsWith: '--') {
				column(name: 'id', index: 1, header: 'id_header')
				column(name: 'emotion', index: 2, header: 'emotion_header')
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof LoadDataAction
		assertEquals 'catalog', action.catalogName
		assertEquals 'schema', action.schemaName
		assertEquals 'monkey', action.tableName
		assertEquals 'data.csv', action.file
		assertTrue action.relativeToChangelogFile
		assertEquals 'UTF-8', action.encoding
		assertEquals ';', action.separator
		assertEquals "'", action.quotchar
		assertNotNull 'LoadDataChange.resourceAccessor should not be null', action.resourceAccessor
		def columns = action.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof LoadDataAction.LoadDataColumn }
		assertEquals 2, columns.size()
		assertEquals 'id', columns[0].name
		assertEquals 1, columns[0].index
		assertEquals 'id_header', columns[0].header
		assertEquals 'emotion', columns[1].name
		assertEquals 2, columns[1].index
		assertEquals 'emotion_header', columns[1].header
		assertNoOutput()
	}

	/**
	 * Test parsing a loadData action when the file name is actually a File
	 * object.  This was deprecated, so make sure we get the expected error.
	 * This test can be removed when we stop explicitly checking this condition
	 * in the delegate.
	 */
	@Test(expected = ParseException)
	void loadDataFullWithFile() {
		def action = parseAction("""
			loadData(catalogName: 'catalog',
					 schemaName: 'schema',
					 tableName: 'monkey',
					 file: new File('data.csv'),
					 encoding: 'UTF-8',
					 separator: ';',
					 quotchar: '"') {
				column(name: 'id')
				column(name: 'emotion')
			}
		""")
	}

	/**
	 * LoadData actions allow columns but not a where clause, so try one that
	 * has a where clause to make sure it is properly rejected.
	 */
	@Test(expected = ParseException)
	void loadDataWithWhereClause() {
		resourceAccessor = new FileSystemResourceAccessor()

		def action = parseAction("""
			loadData(catalogName: 'catalog',
					 schemaName: 'schema',
					 tableName: 'monkey',
					 file: 'data.csv',
					 encoding: 'UTF-8',
					 separator: ';',
					 quotchar: "'") {
				where "invalid"
			""")
	}

	/**
	 * Test parsing a loadData action when the attribute map and column closure
	 * are both empty.  We don't need to worry about the map or the closure
	 * being missing because that kind of action doesn't make sense.  In this
	 * case, Liquibase itself has defaults for the separator and quote chars,
	 * which is what we check in the test.
	 */
	@Test
	void loadUpdateDataEmpty() {
		resourceAccessor = new FileSystemResourceAccessor()
		def action = parseAction("""
			loadUpdateData([:]) {	}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof LoadDataAction
		assertNull action.catalogName
		assertNull action.schemaName
		assertNull action.tableName
		assertNull action.file
		assertNull action.relativeToChangelogFile
		assertNull action.encoding
		assertEquals ",", action.separator
		assertEquals '"', action.quotchar
		assertNull action.primaryKey
		assertFalse action.onlyUpdate // False is the Lioquibase default
		assertNotNull 'LoadDataChange.resourceAccessor should not be null', action.resourceAccessor
		def columns = action.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing a loadUpdateData action with all supported attributes and a
	 * few columns.  We're not too concerned with the column contents, just
	 * make sure we get them.  For this test, we want a separator and quotchar
	 * that is different from the Liquibase defaults, so we'll go with
	 * semi-colon separated and single quoted
	 */
	@Test
	void loadUpdateDataFull() {
		resourceAccessor = new FileSystemResourceAccessor()

		def action = parseAction("""
			loadUpdateData(catalogName: 'catalog',
					       schemaName: 'schema',
					       tableName: 'monkey',
					       file: 'data.csv',
					       relativeToChangelogFile: true,
					       encoding: 'UTF-8',
					       separator: ';',
					       quotchar: "'",
			               primaryKey: 'id',
			               onlyUpdate: true) {
				column(name: 'id')
				column(name: 'emotion')
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof LoadDataAction
		assertEquals 'catalog', action.catalogName
		assertEquals 'schema', action.schemaName
		assertEquals 'monkey', action.tableName
		assertEquals 'data.csv', action.file
		assertTrue action.relativeToChangelogFile
		assertEquals 'UTF-8', action.encoding
		assertEquals ';', action.separator
		assertEquals "'", action.quotchar
		assertEquals 'id', action.primaryKey
		assertTrue action.onlyUpdate
		assertNotNull 'LoadDataChange.resourceAccessor should not be null', action.resourceAccessor
		def columns = action.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof LoadDataAction.LoadDataColumn }
		assertEquals 2, columns.size()
		assertEquals 'id', columns[0].name
		assertEquals 'emotion', columns[1].name
		assertNoOutput()
	}

	/**
	 * Test parsing a loadUpdateData action when the file name is actually a
	 * File object.  This was removed from the delegate, so make sure we get
	 * the expected exception.  This test can be removed when we stop looking
	 * for this condition in the delegate.
	 */
	@Test(expected = ParseException)
	void loadUpdateDataFullWithFile() {
		def action = parseAction("""
			loadUpdateData(catalogName: 'catalog',
					       schemaName: 'schema',
					       tableName: 'monkey',
					       file: new File('data.csv'),
					       encoding: 'UTF-8',
					       separator: ';',
					       quotchar: '"') {
				column(name: 'id')
				column(name: 'emotion')
			}
		""")
	}

	/**
	 * LoadUpdateData actions allow columns but not a where clause, so try one
	 * that has a where clause to make sure it is properly rejected.
	 */
	@Test(expected = ParseException)
	void loadUpdateDataWithWhereClause() {
		resourceAccessor = new FileSystemResourceAccessor()

		def action = parseAction("""
			loadUpdateData(catalogName: 'catalog',
					       schemaName: 'schema',
					       tableName: 'monkey',
					       file: 'data.csv',
					       encoding: 'UTF-8',
					       separator: ';',
					       quotchar: "'") {
				where "invalid"
			}
		""")
	}

	/**
	 * Test an empty output action
	 */
	@Test
	void outputEmpty() {
		def action = parseAction("""
			output([:])
		""")

		assertEquals 1, changeSet.changes.size()
		assertTrue changeSet.action instanceof OutputMessageAction
		assertNull changeSet.action.message
		assertEquals "", changeSet.action.target
		assertNoOutput()
	}

	/**
	 * Test an output action with all supported properties
	 */
	@Test
	void outputFull() {
		def action = parseAction("""
			output([message: 'some helpful message',
			        target: 'STDOUT'])
		""")

		assertEquals 1, changeSet.changes.size()
		assertTrue changeSet.action instanceof OutputMessageAction
		assertEquals 'some helpful message', changeSet.action.message
		assertEquals 'STDOUT', changeSet.action.target
		assertNoOutput()
	}

	/**
	 * Test an empty setColumnRemarks action
	 */
	@Test
	void setColumnRemarksEmpty() {
		def action = parseAction("""
			setColumnRemarks([:])
		""")
	}

	/**
	 * Test an output action with all supported properties
	 */
	@Test
	void setColumnRemarksFull() {
		def action = parseAction("""
			setColumnRemarks(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					columnName: 'emotion',
					remarks: 'some helpful message'
			)
		""")

		assertEquals 1, changeSet.changes.size()
		assertTrue changeSet.action instanceof AlterRemarksAction
		assertEquals 'catalog', changeSet.action.catalogName
		assertEquals 'schema', changeSet.action.schemaName
		assertEquals 'monkey', changeSet.action.tableName
		assertEquals 'emotion', changeSet.action.columnName
		assertEquals 'some helpful message', changeSet.action.remarks
		assertNoOutput()
	}

	/**
	 * Test an empty setColumnRemarks action
	 */
	@Test
	void setTableRemarksEmpty() {
		def action = parseAction("""
			setTableRemarks([:])
		""")
	}

	/**
	 * Test an output action with all supported properties
	 */
	@Test
	void setTableRemarksFull() {
		def action = parseAction("""
			setTableRemarks(
					catalogName: 'catalog',
					schemaName: 'schema',
					tableName: 'monkey',
					remarks: 'some helpful message'
			)
		""")

		assertEquals 1, changeSet.changes.size()
		assertTrue changeSet.action instanceof AlterRemarksAction
		assertEquals 'catalog', changeSet.action.catalogName
		assertEquals 'schema', changeSet.action.schemaName
		assertEquals 'monkey', changeSet.action.tableName
		assertEquals 'some helpful message', changeSet.action.remarks
		assertNoOutput()
	}

	/**
	 * Test parsing a stop action with an empty parameter map.  In this case, we
	 * expect Liquibase to give us a default message.
	 */
	@Test
	void stopEmpty() {
		def action = parseAction("""
			stop([:])
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof ThrowExceptionAction
		assertEquals 'Stop command in changelog file', action.message
		assertNoOutput()
	}

	/**
	 * Test parsing a stop action when the message is in the attributes.
	 */
	@Test
	void stopMessageInAttributes() {
		def action = parseAction("""
			stop(message: 'Stop the refactoring. Just...stop.')
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof ThrowExceptionAction
		assertEquals 'Stop the refactoring. Just...stop.', action.message
		assertNoOutput()
	}

	/**
	 * Test parsing a stop action when the message is not in an attribute.
	 */
	@Test
	void stopMessageIsArgument() {
		def action = parseAction("""
			stop 'Stop the refactoring. Just...stop.'
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof ThrowExceptionAction
		assertEquals 'Stop the refactoring. Just...stop.', action.message
		assertNoOutput()
	}

	/**
	 * Test parsing a tagDatabase action when we have no attributes to make sure
	 * the DSL doesn't introduce any defaults.
	 */
//	@Test
//	void tagDatabaseEmpty() {
//		buildChangeSet {
//			tagDatabase([:])
//		}
//
//		assertEquals 0, changeSet.rollback.changes.size()
//		def changes = changeSet.changes
//		assertNotNull changes
//		assertEquals 1, changes.size()
//		assertTrue action instanceof Tag
//		assertNull action.tag
//		assertNoOutput()
//	}

	/**
	 * Test parsing a tagDatabase action when we have all supported attributes.
	 */
//	@Test
//	void tagDatabaseNameInAttributes() {
//		buildChangeSet {
//			tagDatabase(tag: 'monkey')
//		}
//
//		assertEquals 0, changeSet.rollback.changes.size()
//		def changes = changeSet.changes
//		assertNotNull changes
//		assertEquals 1, changes.size()
//		assertTrue action instanceof TagDatabaseChange
//		assertEquals 'monkey', action.tag
//		assertNoOutput()
//	}

	/**
	 * Test parsing a tagDatabase action when the name is not in an attribute.
	 */
//	@Test
//	void tagDatabaseNameIsArgument() {
//		buildChangeSet {
//			tagDatabase 'monkey'
//		}
//
//		assertEquals 0, changeSet.rollback.changes.size()
//		def changes = changeSet.changes
//		assertNotNull changes
//		assertEquals 1, changes.size()
//		assertTrue action instanceof TagDatabaseChange
//		assertEquals 'monkey', action.tag
//		assertNoOutput()
//	}

	/**
	 * test parsing an updateData action with no attributes and no closure to
	 * make sure the DSL is not adding any unintended defaults.
	 */
	@Test
	void updateDataEmpty() {
		def action = parseAction("""
			update([:]) {	}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof UpdateDataAction
		assertNull action.catalogName
		assertNull action.schemaName
		assertNull action.tableName
		assertNull action.where
		def columns = action.columns
		assertNotNull columns
		assertEquals 0, columns.size()
		assertNoOutput()
	}

	/**
	 * Test parsing an updateData action when we have all supported attributes,
	 * and a couple of columns, but no where clause.  This should not cause an
	 * issue, since it is legal to update all rows in a table. As always, we
	 * don't care about the contents of the columns.
	 */
	@Test
	void updateDataNoWhere() {
		def action = parseAction("""
			update(catalogName: 'catalog',  schemaName: 'schema', tableName: 'monkey') {
				column(name: 'rfid_tag')
				column(name: 'emotion')
				column(name: 'last_updated')
				column(name: 'active')
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof UpdateDataAction
		assertEquals 'catalog', action.catalogName
		assertEquals 'schema', action.schemaName
		assertEquals 'monkey', action.tableName
		assertNull action.where
		def columns = action.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof Column }
		assertEquals 4, columns.size()
		assertEquals 'rfid_tag', columns[0].name
		assertEquals 'emotion', columns[1].name
		assertEquals 'last_updated', columns[2].name
		assertEquals 'active', columns[3].name
		assertNoOutput()
	}

	/**
	 * Test parsing an updateData action when we have attributes, columns and
	 * a where clause.  We won't test a where and no columns because that
	 * action doesn't make sense, and will be rejected by Liquibase itself.
	 */
	@Test
	void updateDataFull() {
		def action = parseAction("""
			update(catalogName: 'catalog',  schemaName: 'schema', tableName: 'monkey') {
				column(name: 'rfid_tag')
				column(name: 'emotion')
				column(name: 'last_updated')
				column(name: 'active')
				where "id=882"
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof UpdateDataAction
		assertEquals 'catalog', action.catalogName
		assertEquals 'schema', action.schemaName
		assertEquals 'monkey', action.tableName
		assertEquals 'id=882', action.where
		def columns = action.columns
		assertNotNull columns
		assertTrue columns.every { column -> column instanceof Column }
		assertEquals 4, columns.size()
		assertEquals 'rfid_tag', columns[0].name
		assertEquals 'emotion', columns[1].name
		assertEquals 'last_updated', columns[2].name
		assertEquals 'active', columns[3].name
		assertNoOutput()
	}
}

