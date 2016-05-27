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

import liquibase.action.ExecuteSqlAction
import liquibase.action.QuerySqlAction
import liquibase.action.core.CustomClassAction
import liquibase.action.core.ExecuteShellCommandAction
import liquibase.action.core.ExecuteSqlFileAction
import liquibase.exception.ParseException

//import liquibase.sql.visitor.PrependSqlVisitor
import org.junit.Test
import org.liquibase.groovy.change.CustomProgrammaticChangeWrapper

import static org.junit.Assert.*
import liquibase.resource.FileSystemResourceAccessor

/**
 * This is one of several classes that test the creation of refactoring actions
 * for ChangeSets. This particular class tests custom changes such as
 * {@code sql} and {@code executeCommand}
 * <p>
 * Since the Groovy DSL parser is meant to act as a pass-through for Liquibase
 * itself, it doesn't do much in the way of error checking.  For example, we
 * aren't concerned with whether or not required attributes are present - we
 * leave that to Liquibase itself.  In general, each action will have 3 kinds
 * of tests:<br>
 * <ol>
 * <li>A test with an empty parameter map, and if supported, an empty closure.
 * This kind of test will make sure that the Groovy parser doesn't introduce
 * any unintended attribute defaults for a change.</li>
 * <li>A test that uses an action that would valid except for an extra invalid
 * attribute.  This makes sure that Liquibase will reject unknown attributes
 * instead of silently ignoring them.</li>
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
class CustomRefactoringTests extends IntegrationTest {

	/**
	 * Test a customChange with no map or closure.
	 */
	@Test
	void customChangeEmpty() {
		def action = parseAction("""
			customChange([:])
		""")

		assertTrue action instanceof CustomClassAction
		assertNull action.customClass
		assertEquals 0, action.parameters.size()
		assertNoOutput()
	}

	/**
	 * Test a customChange with an invalid attribute.
	 */
	@Test(expected = ParseException)
	void customChangeInvalid() {
		parseAction("""
			customChange(class: 'org.liquibase.change.custom.MonkeyChange',
			             invalidAttr: 'invalid')
		""")
	}

	/**
	 * Test a custom with a class name, but on parameters.
	 */
	@Test
	void customChangeWithClassAndNoParameters() {
		def action = parseAction("""
			customChange(class: 'org.liquibase.change.custom.MonkeyChange')
		""")

		assertTrue action instanceof CustomClassAction
		assertEquals 'org.liquibase.change.custom.MonkeyChange', action.customClass.name
		assertEquals 0, action.parameters.size()
		assertNoOutput()
	}

	/**
	 * Test a customChange with a class name and 2 parameters.
	 */
	@Test
	void customChangeWithClassAndParameters() {
		def action = parseAction("""
			customChange(class: 'org.liquibase.change.custom.MonkeyChange') {
				emotion('angry')
				'rfid-tag'(28763)
			}
		""")

		assertTrue action instanceof CustomClassAction
		assertEquals 'org.liquibase.change.custom.MonkeyChange', action.customClass.name
		def args = action.parameters
		assertNotNull args
		assertEquals 2, args.size()
		assertEquals 'emotion', args[0].name
		assertEquals 'angry', args[0].value
		assertEquals 'rfid-tag', args[1].name
		assertEquals 28763, args[1].value
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand with no args and an empty closure to make
	 * sure the DSL doesn't introduce any unintended defaults.
	 */
	@Test
	void executeCommandEmptyMapEmptyClosure() {
		def action = parseAction("""
			executeCommand([:]) {}
		""")

		assertTrue action instanceof ExecuteShellCommandAction
		assertNull action.executable
		assertNotNull action.osFilters
		assertEquals 0, action.osFilters.size()
		def args = action.args
		assertNotNull args
		assertEquals 0, args.size()
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand change when we have no attributes and there
	 * is no closure.
	 */
	@Test
	void executeCommandEmptyMapNoClosure() {
		def action = parseAction("""
			executeCommand([:])
		""")

		assertTrue action instanceof ExecuteShellCommandAction
		assertNull action.executable
		assertNotNull action.osFilters
		assertEquals 0, action.osFilters.size()
		def args = action.args
		assertNotNull args
		assertEquals 0, args.size()
		assertNoOutput()
	}

	/**
	 * Test parsing executeCommand when we have an invalid attribute.
	 */
	@Test(expected = ParseException)
	void executeCommandInvalidAttribute() {
		parseAction("""
			executeCommand(executable: "awk '/monkey/ { count++ } END { print count }'",
			        invalidAttr: 'invalid',
					os: 'Mac OS X, Linux')
		""")
	}

	/**
	 * Test parsing executeCommand when we have all supported attributes,but
	 * no argument closure.
	 */
	@Test
	void executeCommandWithNoArgs() {
		def action = parseAction("""
			executeCommand(executable: "awk '/monkey/ { count++ } END { print count }'",
					os: 'Mac OS X, Linux')
		""")

		assertTrue action instanceof ExecuteShellCommandAction
		assertEquals "awk '/monkey/ { count++ } END { print count }'", action.executable
		assertNotNull action.osFilters
		assertEquals 1, action.osFilters.size
		assertEquals '[Mac OS X, Linux]', action.osFilters[0]
		def args = action.args
		assertNotNull args
		assertEquals 0, args.size()
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand change where the arguments are maps, like
	 * the XML would do it.
	 */
	@Test
	void executeCommandWithArgsInMap() {
		def action = parseAction("""
			executeCommand(executable: "awk", os: 'Mac OS X, Linux') {
				arg(value: '/monkey/ { count++ } END { print count }')
				arg(value: '-f database.log')
			}
		""")

		assertTrue action instanceof ExecuteShellCommandAction
		assertEquals "awk", action.executable
		assertNotNull action.osFilters
		assertEquals 1, action.osFilters.size
		assertEquals '[Mac OS X, Linux]', action.osFilters[0]
		def args = action.args
		assertNotNull args
		assertEquals 2, args.size()
		assertTrue args.every { arg -> arg instanceof String }
		assertEquals '/monkey/ { count++ } END { print count }', args[0]
		assertEquals '-f database.log', args[1]
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand change where the arguments are just Strings.
	 * This is not the way the XML does it, but it is the way the Groovy DSL has
	 * always done it, and it is nice shorthand.
	 */
	@Test
	void executeCommandWithStringArgs() {
		def action = parseAction("""
			executeCommand(executable: "awk", os: 'Mac OS X, Linux') {
				arg '/monkey/ { count++ } END { print count }'
				arg '-f database.log'
			}
		""")

		assertTrue action instanceof ExecuteShellCommandAction
		assertEquals "awk", action.executable
		assertNotNull action.osFilters
		assertEquals 1, action.osFilters.size
		assertEquals '[Mac OS X, Linux]', action.osFilters[0]
		def args = action.args
		assertNotNull args
		assertEquals 2, args.size()
		assertTrue args.every { arg -> arg instanceof String }
		assertEquals '/monkey/ { count++ } END { print count }', args[0]
		assertEquals '-f database.log', args[1]
		assertNoOutput()
	}

	/**
	 * Make sure modifySql works.  Most of the tests for this are in
	 * {@link ModifySqlDelegateTests}, this just needs to make sure that the
	 * SqlVisitors that the delegate returns are added to the changeSet.  This
	 * one also tests that we can have a modifySql with no attributes of its own.
	 */
	@Test
	void modifySqlValid() {
		def action = parseAction("""
			modifySql {
				prepend(value: 'engine INNODB')
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 0, changes.size()
		assertEquals 1, changeSet.sqlVisitors.size()
//		assertTrue changeSet.sqlVisitors[0] instanceof PrependSqlVisitor
		assertEquals 'engine INNODB', changeSet.sqlVisitors[0].value
		assertNull changeSet.sqlVisitors[0].applicableDbms
		assertNull changeSet.sqlVisitors[0].contexts
		assertFalse changeSet.sqlVisitors[0].applyToRollback
		assertNoOutput()
	}

	/**
	 * Test parsing a sql action when we have an empty attribute map and an
	 * empty closure to make sure we don't get any unintended defaults. Also
	 * test our assumption that Liquibase will default splitStatements and
	 * stripComments to null.
	 */
	@Test
	void sqlEmpty() {
		def action = parseAction("""
			sql([:]) {}
		""")

		assertTrue action instanceof ExecuteSqlAction
		assertNotNull action.dbmsFilters
		assertEquals 0, action.dbmsFilters.size()
		assertNull action.endDelimiter
		assertNull action.splitStatements
		assertNull action.stripComments
		assertNull action.sql
//		assertNull action.comment
		assertNoOutput()
	}

	/**
	 * Test parsing a sql action that has an invalid attribute.
	 */
	@Test(expected = ParseException)
	void sqlInvalid() {
		parseAction("""
			sql(dbms: 'oracle',
				splitStatements: false,
				stripComments: true,
				endDelimiter: '!',
				invalidAttr: 'invalid') {
				"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		""")
	}

	/**
	 * Test parsing a sql action when we have no attributes or a closure, just
	 * a string.
	 */
	@Test
	void sqlIsString() {
		def action = parseAction("""
			sql "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
		""")

		assertTrue action instanceof ExecuteSqlAction
		assertNotNull action.dbmsFilters
		assertEquals 0, action.dbmsFilters.size()
		assertNull action.endDelimiter
		assertNull action.splitStatements
		assertNull action.stripComments
		assertEquals "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", action.sql.toString()
//		assertNull action.comment
		assertNoOutput()
	}

	/**
	 * test parsing a sql action where we only have SQL in a closure.
	 */
	@Test
	void sqlInClosure() {
		def action = parseAction("""
			sql {
				"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		""")

		assertTrue action instanceof ExecuteSqlAction
		assertNotNull action.dbmsFilters
		assertEquals 0, action.dbmsFilters.size()
		assertNull action.endDelimiter
		assertNull action.splitStatements
		assertNull action.stripComments
		assertEquals "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", action.sql.toString()
//		assertNull action.comment
		assertNoOutput()
	}

	/**
	 * Test parsing a sql action when we have no attributes, but we do have a
	 * comment in the closure.
	 */
	@Test
	void sqlCommentInClosure() {
		def action = parseAction("""
			sql {
				comment("No comment")
				"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof ExecuteSqlAction
		assertNull action.dbms
		assertNull action.endDelimiter
		assertTrue action.splitStatements
		assertFalse action.stripComments
		assertEquals "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", action.sql
		assertEquals "No comment", action.comment
		assertNoOutput()
	}

	/**
	 * Test parsing a sql action when we have all supported attributes present
	 * and no comments in the closure.  For this test set the two booleans to
	 * different values.
	 */
	@Test
	void sqlFullWithNoComments() {
		def action = parseAction("""
			sql(dbms: 'oracle',
				splitStatements: false,
				stripComments: true,
				endDelimiter: '!') {
				"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		""")

		assertTrue action instanceof ExecuteSqlAction
		assertNotNull action.dbmsFilters
		assertEquals 1, action.dbmsFilters.size()
		assertEquals '[oracle]', action.dbmsFilters[0]
		assertFalse action.splitStatements
		assertTrue action.stripComments
		assertEquals '!', action.endDelimiter
		assertEquals "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", action.sql.toString()
//		assertNull action.comment
		assertNoOutput()
	}

	/**
	 * Test parsing a sql action when we have all attributes and we have a comment
	 * in the closure.  For this test we only set splitStatements to true.
	 */
	@Test
	void sqlFullWithComments() {
		def action = parseAction("""
			sql(dbms: 'oracle',
				splitStatements: false,
				stripComments: true,
				endDelimiter: '!') {
				comment("No comment")
				"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		""")

		assertEquals 0, changeSet.rollback.changes.size()
		def changes = changeSet.changes
		assertNotNull changes
		assertEquals 1, changes.size()
		assertTrue action instanceof ExecuteSqlAction
		assertEquals 'oracle', action.dbms
		assertFalse action.splitStatements
		assertTrue action.stripComments
		assertEquals '!', action.endDelimiter
		assertEquals "UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", action.sql
		assertEquals "No comment", action.comment
		assertNoOutput()
	}

	/**
	 * Test parsing a sqlFile change with minimal attributes to confirm our
	 * assumptions about Liquibase defaults, which we assume to be true for
	 * splitStatements and false for stripComments.  We can't test this one with
	 * totally empty attributes because a sqlFile change will attempt to open the
	 * file immediately to work around a Liquibase bug.  This also means the file
	 * in question must exist.
	 */
	@Test
	void sqlFileEmpty() {
		def action = parseAction("""
			sqlFile(path: 'src/test/changelog/file.sql')
		""")

		assertTrue action instanceof ExecuteSqlFileAction
		assertEquals 'src/test/changelog/file.sql', action.path
//		assertNull action.relativeToChangelogFile
		assertNull action.encoding
		assertNull action.stripComments
		assertNull action.splitStatements
		assertNull action.endDelimiter
		assertNull action.dbms
		assertNoOutput()
	}

	/**
	 * Test parsing a sqlFile action when we have all an invalid attribute.
	 */
	@Test(expected = ParseException)
	void sqlFileInvalid() {
		parseAction("""
			sqlFile(path: 'src/test/changelog/file.sql',
					relativeToChangelogFile: false,
					stripComments: true,
					splitStatements: false,
					encoding: 'UTF-8',
					endDelimiter: '@',
					dbms: 'oracle',
					invalidAttr: 'invalid')
		""")
	}
	/**
	 * Test parsing a sqlFile action when we have all supported options. For this
	 * test, we set the two booleans to be the opposite of their default values.
	 */
	@Test
	void sqlFileFull() {
		def action = parseAction("""
			sqlFile(path: 'src/test/changelog/file.sql',
					relativeToChangelogFile: false,
					stripComments: true,
					splitStatements: false,
					encoding: 'UTF-8',
					endDelimiter: '@',
					dbms: 'oracle')
		""")

		assertTrue action instanceof ExecuteSqlFileAction
		assertEquals 'src/test/changelog/file.sql', action.path
//		assertFalse action.relativeToChangelogFile
		assertEquals 'UTF-8', action.encoding
		assertTrue action.stripComments
		assertFalse action.splitStatements
		assertEquals '@', action.endDelimiter
		assertEquals 'oracle', action.dbms
		assertNoOutput()
	}
}

