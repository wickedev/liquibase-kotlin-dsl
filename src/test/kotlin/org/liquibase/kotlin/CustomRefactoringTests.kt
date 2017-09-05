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

import liquibase.change.core.ExecuteShellCommandChange
import liquibase.change.core.RawSQLChange
import liquibase.change.core.SQLFileChange
import liquibase.change.custom.CustomChangeWrapper
import liquibase.sql.visitor.PrependSqlVisitor
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.*

/**
 * This is one of several classes that test the creation of refactoring changes
 * for ChangeSets. This particular class tests custom changes such as
 * {@code sql} and {@code executeCommand}
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
class CustomRefactoringTests : ChangeSetTests() {

	@Test
	fun customRefactoringWithClassAndNoParameters() {
		buildChangeSet {
			customChange(className = "org.liquibase.change.custom.MonkeyChange")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CustomChangeWrapper>(changes[0]) {
			assertEquals("org.liquibase.change.custom.MonkeyChange", it.className)
			assertNotNull(it.resourceAccessor)
			assertNotNull(it.customChange)
		}
		assertNoOutput()
	}


	@Test
	fun customRefactoringWithClassAndParameters() {
		buildChangeSet {
			customChange(className = "org.liquibase.change.custom.MonkeyChange") {
				param("emotion", "angry")
				param("rfid-tag", 28763)
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<CustomChangeWrapper>(changes[0]) {
			assertEquals("org.liquibase.change.custom.MonkeyChange", it.className)
			assertNotNull(it.resourceAccessor)
			assertEquals(it.getParamValue("emotion"), "angry")
			assertEquals(it.getParamValue("rfid-tag"), "28763")
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand with no args and an empty closure to make
	 * sure the DSL doesn't introduce any unintended defaults.
	 */
	@Test
	fun executeCommandEmptyMapEmptyClosure() {
		buildChangeSet {
			executeCommand(executable = "") {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<ExecuteShellCommandChange>(changes[0]) {
			assertEquals("", it.executable)
			assertNull(it.os)
			val args = it.args
			assertNotNull(args)
			assertEquals(0, args.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand change when we have no attributes and there
	 * is no closure.
	 */
	@Test
	fun executeCommandEmptyMapNoClosure() {
		buildChangeSet {
			executeCommand(executable = "")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<ExecuteShellCommandChange>(changes[0]) {
			assertEquals("", it.executable)
			assertNull(it.os)
			val args = it.args
			assertNotNull(args)
			assertEquals(0, args.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing executeCommand when we have all supported attributes,but
	 * no argument closure.
	 */
	@Test
	fun executeCommandWithNoArgs() {
		buildChangeSet {
			executeCommand(executable = "awk '/monkey/ { count++ } END { print count }'",
					os = "Mac OS X, Linux")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<ExecuteShellCommandChange>(changes[0]) {
			assertEquals("awk '/monkey/ { count++ } END { print count }'", it.executable)
			assertNotNull(it.os)
			assertEquals(2, it.os.size)
			assertEquals("Mac OS X", it.os[0])
			assertEquals("Linux", it.os[1])
			val args = it.args
			assertNotNull(args)
			assertEquals(0, args.size)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing an executeCommand change where the arguments are just Strings.
	 * This is not the way the XML does it, but it is the way the Kotlin DSL has
	 * always done it, and it is nice shorthand.
	 */
	@Test
	fun executeCommandWithArgsInMap() {
		buildChangeSet {
			executeCommand(executable = "awk", os = "Mac OS X, Linux") {
				arg("/monkey/ { count++ } END { print count }")
				arg("-f database.log")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<ExecuteShellCommandChange>(changes[0]) {
			assertEquals("awk", it.executable)
			assertNotNull(it.os)
			assertEquals(2, it.os.size)
			assertEquals("Mac OS X", it.os[0])
			assertEquals("Linux", it.os[1])
			val args = it.args
			assertNotNull(args)
			assertEquals(2, args.size)
			assertEquals("/monkey/ { count++ } END { print count }", args[0])
			assertEquals("-f database.log", args[1])
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Make sure modifySql works.  Most of the tests for this are in
	 * {@link ModifySqlDelegateTests}, this just needs to make sure that the
	 * SqlVisitors that the delegate returns are added to the changeSet.  This
	 * one also tests that we can have a modifySql with no attributes of its own.
	 */
	@Test
	fun modifySqlValid() {
		buildChangeSet {
			modifySql {
				prepend(value = "engine INNODB")
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(0, changes.size)
		assertEquals(1, changeSet.sqlVisitors.size)
		assertType<PrependSqlVisitor>(changeSet.sqlVisitors[0]) {
			assertEquals("engine INNODB", it.value)
			assertNull(it.applicableDbms)
			assertNull(it.contexts)
			assertFalse(it.isApplyToRollback)
		}
		assertNoOutput()


	}

	/**
	 * Test parsing a sql change when we have an empty attribute map and an
	 * empty closure to make sure we don't get any unintended defaults. Also test
	 * our assumption that Liquibase will default splitStatements to true and
	 * stripComments to false.
	 */
	@Test
	fun sqlWithoutAttributesOrClosure() {
		buildChangeSet {
			sql {}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertNull(it.dbms)
			assertNull(it.endDelimiter)
			assertTrue(it.isSplitStatements)
			assertFalse(it.isStripComments)
			assertNull(it.sql)
			assertNull(it.comment)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a sql change when we have no attributes or a closure, just
	 * a string.
	 */
	@Test
	fun sqlIsString() {
		buildChangeSet {
			sql("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertNull(it.dbms)
			assertNull(it.endDelimiter)
			assertTrue(it.isSplitStatements)
			assertFalse(it.isStripComments)
			assertEquals("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", it.sql)
			assertNull(it.comment)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * test parsing a sql change where we only have SQL in a closure.
	 */
	@Test
	fun sqlInClosure() {
		buildChangeSet {
			sql {
				-"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertNull(it.dbms)
			assertNull(it.endDelimiter)
			assertTrue(it.isSplitStatements)
			assertFalse(it.isStripComments)
			assertEquals("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", it.sql)
			assertNull(it.comment)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a sql change when we have no attributes, but we do have a
	 * comment in the closure.
	 */
	@Test
	fun sqlCommentInClosure() {
		buildChangeSet {
			sql {
				comment("No comment")
				-"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertNull(it.dbms)
			assertNull(it.endDelimiter)
			assertTrue(it.isSplitStatements)
			assertFalse(it.isStripComments)
			assertEquals("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", it.sql)
			assertEquals("No comment", it.comment)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a sql chanve when we have all supported attributes present
	 * and no comments in the closure.  For this test set the two booleans to
	 * the opposite of the Liquibase defaults.
	 */
	@Test
	fun sqlFullWithNoComments() {
		buildChangeSet {
			sql(dbms = "oracle",
					splitStatements = false,
					stripComments = true,
					endDelimiter = "!") {
				-"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertEquals("oracle", it.dbms)
			assertFalse(it.isSplitStatements)
			assertTrue(it.isStripComments)
			assertEquals("!", it.endDelimiter)
			assertEquals("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", it.sql)
			assertNull(it.comment)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a sql change when we have all attributes and we have a comment
	 * in the closure.  For this test we only set splitStatements to true.
	 */
	@Test
	fun sqlFullWithComments() {
		buildChangeSet {
			sql(dbms = "oracle",
					splitStatements = false,
					stripComments = true,
					endDelimiter = "!") {
				comment("No comment")
				-"UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)"
			}
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<RawSQLChange>(changes[0]) {
			assertEquals("oracle", it.dbms)
			assertFalse(it.isSplitStatements)
			assertTrue(it.isStripComments)
			assertEquals("!", it.endDelimiter)
			assertEquals("UPDATE monkey SET emotion='ANGRY' WHERE id IN (1,2,3,4,5)", it.sql)
			assertEquals("No comment", it.comment)
			assertNotNull(it.resourceAccessor)
		}
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
	fun sqlFileEmpty() {
		buildChangeSet {
			sqlFile(path = "src/test/changelog/file.sql")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<SQLFileChange>(changes[0]) {
			assertEquals("src/test/changelog/file.sql", it.path)
			assertNull(it.isRelativeToChangelogFile)
			assertNull(it.encoding)
			assertFalse(it.isStripComments)
			assertTrue(it.isSplitStatements)
			assertNull(it.endDelimiter)
			assertNull(it.dbms)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}

	/**
	 * Test parsing a sqlFile change when we have all supported options. For this
	 * test, we set the two booleans to be the opposite of their default values.
	 */
	@Test
	fun sqlFileFull() {
		buildChangeSet {
			sqlFile(path = "src/test/changelog/file.sql",
					relativeToChangelogFile = false,
					stripComments = true,
					splitStatements = false,
					encoding = "UTF-8",
					endDelimiter = "@",
					dbms = "oracle")
		}

		assertEquals(0, changeSet.rollback.changes.size)
		val changes = changeSet.changes
		assertNotNull(changes)
		assertEquals(1, changes.size)
		assertType<SQLFileChange>(changes[0]) {
			assertEquals("src/test/changelog/file.sql", it.path)
			assertFalse(it.isRelativeToChangelogFile)
			assertEquals("UTF-8", it.encoding)
			assertTrue(it.isStripComments)
			assertFalse(it.isSplitStatements)
			assertEquals("@", it.endDelimiter)
			assertEquals("oracle", it.dbms)
			assertNotNull(it.resourceAccessor)
		}
		assertNoOutput()
	}
}

