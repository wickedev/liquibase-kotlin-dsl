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

import liquibase.change.CheckSum
import liquibase.change.core.*
import liquibase.exception.RollbackImpossibleException
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * This class tests the methods of a change set that are neither preconditions,
 * nor refactorings.  This includes things like {@code rollback}
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ChangeSetMethodTests : ChangeSetTests() {
	@Test
	fun comments() {
		buildChangeSet {
			comment("This is a comment")
		}
		assertEquals("This is a comment", changeSet.comments)
		assertNoOutput()
	}

	/**
	 * Test the validChecksuum functionality.  This test needs some explanation.
	 * Liquibase's {@code isChecksumValid()} method compares the change set's
	 * current checksum to the hash given to the method. If they don't match, it
	 * will check the current checksum against checksums that are stored with the
	 * validChecksum element.  So to test this, we do the following:<br>
	 * <ol>
	 * <li>Call isChecksumValid with a bogus checksum.  Because the bogus checksum
	 * does not match the current checksum, we expect it to return false.</li>
	 * <li>apply the validCheckSum method with the current, valid, checksum for
	 * the changeSet.</li>
	 * <li>Call isChecksumValid with the same bogus checksum as before. It still
	 * won't match the current calculated checksum, but since that checksum has
	 * been marked as valid, we should now get a true result.
	 */
	@Test
	fun validChecksumTest() {
		val checksum = "d0763edaa9d9bd2a9516280e9044d885"
		val liquibaseChecksum = CheckSum.parse(checksum)
		val goodChecksum = changeSet.generateCheckSum().toString()
		assertFalse(changeSet.isCheckSumValid(liquibaseChecksum), "Arbitrary checksum should not be valid before being added")
		buildChangeSet {
			validCheckSum(goodChecksum)
		}
		assertTrue(changeSet.isCheckSumValid(liquibaseChecksum), "Arbitrary checksum should be valid after being added")
		assertNoOutput()
	}

	/**
	 * Test a rollback with a single statement passed as a string.  In this
	 * test, we expect a null resource accessor because they are not needed
	 * for rollbacks that are just SQL, and Liquibase doesn't give us an easy
	 * way to pass in a resource accessor.
	 */
	@Test
	fun rollbackString() {
		val rollbackSql = "DROP TABLE monkey"
		buildChangeSet {
			rollback {
				-rollbackSql
			}
		}

		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(1, changes.size)
		assertEquals(RawSQLChange("DROP TABLE monkey").sql, (changes[0] as RawSQLChange).sql)
		assertNoOutput()
	}

	/**
	 * Test rollback with two statements passed as strings. In this test, we
	 * expect a null resource accessor because they are not needed for
	 * rollbacks that are just SQL, and Liquibase doesn't give us an easy way
	 * to pass in a resource accessor.
	 */
	@Test
	fun rollbackTwoStrings() {
		val rollbackSql = """UPDATE monkey_table SET emotion='angry' WHERE status='PENDING';
ALTER TABLE monkey_table DROP COLUMN angry;"""
		buildChangeSet {
			rollback(sql = rollbackSql)
		}
		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(2, changes.size)
		assertEquals(RawSQLChange("UPDATE monkey_table SET emotion='angry' WHERE status='PENDING'").sql, (changes[0] as RawSQLChange).sql)
		assertEquals(RawSQLChange("ALTER TABLE monkey_table DROP COLUMN angry").sql, (changes[1] as RawSQLChange).sql)
		assertNoOutput()
	}

	/**
	 * Rollback one statement given in a closure.  In this test, we expect a
	 * null resource accessor because they are not needed for rollbacks that
	 * are just SQL, and Liquibase doesn't give us an easy way to pass in a
	 * resource accessor.
	 */
	@Test
	fun rollbackOneStatementInClosure() {
		buildChangeSet {
			rollback {
				-"UPDATE monkey_table SET emotion='angry' WHERE status='PENDING'"
			}
		}

		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(1, changes.size)
		assertEquals(RawSQLChange("UPDATE monkey_table SET emotion='angry' WHERE status='PENDING'").sql, (changes[0] as RawSQLChange).sql)
		assertNoOutput()
	}

	/**
	 * Rollback two statements given in a closure. In this test, we expect a
	 * null resource accessor because they are not needed for rollbacks that
	 * are just SQL, and Liquibase doesn't give us an easy way to pass in a
	 * resource accessor.
	 */
	@Test
	fun rollbackTwoStatementInClosure() {
		buildChangeSet {
			rollback(sql = """UPDATE monkey_table SET emotion='angry' WHERE status='PENDING';
ALTER TABLE monkey_table DROP COLUMN angry;""")
		}

		assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(2, changes.size)
		assertEquals(RawSQLChange("UPDATE monkey_table SET emotion='angry' WHERE status='PENDING'").sql, (changes[0] as RawSQLChange).sql)
		assertEquals(RawSQLChange("ALTER TABLE monkey_table DROP COLUMN angry").sql, (changes[1] as RawSQLChange).sql)
		assertNoOutput()
	}

	/**
	 * Rollback one change when the change is a nested refactoring.
	 */
	@Test
	fun rollbackOneNestedChange() {
		buildChangeSet {
			rollback {
				delete(tableName = "monkey")
			}
		}

		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(1, changes.size)
		assertTrue(changes[0] is DeleteDataChange)
		assertEquals("monkey", (changes[0] as DeleteDataChange).tableName)
		assertNoOutput()
	}

	/**
	 * Rollback with two changes when they are both given as nested refactorings.
	 * We don't care too much about the content of the resultant changes, just
	 * that the right changes of the right type wind up in the right place.
	 */
	@Test
	fun rollbackTwoNestedChange() {
		buildChangeSet {
			rollback {
				update(tableName = "monkey") {
					column(name = "emotion", value = "angry")
				}
				dropColumn(tableName = "monkey", columnName = "emotion")
			}
		}

		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(2, changes.size)
		assertTrue(changes[0] is UpdateDataChange)
		assertEquals("monkey", (changes[0] as UpdateDataChange).tableName)
		assertTrue(changes[1] is DropColumnChange)
		assertEquals("monkey", (changes[1] as DropColumnChange).tableName)
		assertEquals("emotion", (changes[1] as DropColumnChange).columnName)
		assertNoOutput()
	}

	/**
	 * This is a wacky combination. Let's use a refactoring paired with raw SQL.
	 * I don't know wha the XML parser does, but the Kotlin parser can handle
	 * it.  In this case, we expect a resource accessor to be set in the
	 * change based rollback, but not the SQL based one.
	 */
	@Test
	fun rollbackCombineRefactoringWithSql() {
		buildChangeSet {
			rollback {
				update(tableName = "monkey") {
					column(name = "emotion", value = "angry")
				}
				-"ALTER TABLE monkey_table DROP COLUMN angry"
			}
		}

		Assert.assertEquals(0, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(2, changes.size)
		assertTrue(changes[0] is UpdateDataChange)
		assertEquals("monkey", (changes[0] as UpdateDataChange).tableName)
		assertEquals(RawSQLChange("ALTER TABLE monkey_table DROP COLUMN angry").sql, (changes[1] as RawSQLChange).sql)
		assertNoOutput()
	}

	/**
	 * Process a map based rollback that is missing the changeSetId.  Expect an
	 * error.
	 */
	@Test(expected = RollbackImpossibleException::class)
	fun rollbackMissingId() {
		buildChangeSet {
			rollback(changeSetAuthor = "darwin")
		}
	}

	/**
	 * Process a map based rollback when the referenced change cannot be found.
	 */
	@Test(expected = RollbackImpossibleException::class)
	fun rollbackInvalidChange() {
		buildChangeSet {
			rollback(changeSetId = "big-bang", changeSetAuthor = CHANGESET_AUTHOR)
		}
	}

	// rollback map without path /filePath
	/**
	 * Process a map based rollback when we don't supply a file path.  In that
	 * case, we should use the one in the databaseChangeLog.  This test needs to
	 * use the same constants in the rollback as was used to create the changeSet
	 * in the set up.
	 */
	@Test
	fun rollbackWithoutPath() {
		buildChangeSet {
			addColumn(tableName = "monkey") {
				column(name = "diet", type = "varchar(30)")
			}
			rollback(changeSetId = CHANGESET_ID, changeSetAuthor = CHANGESET_AUTHOR)
		}

		// in this case, we expect the addColumn change to also be the change
		// inside the rollback.
		Assert.assertEquals(1, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(1, changes.size)
		assertTrue(changes[0] is AddColumnChange)
		assertEquals("monkey", (changes[0] as AddColumnChange).tableName)
		assertNoOutput()
	}

	/**
	 * Test a map based rollback that includes a path.  We can't really test this
	 * easily because we need a valid change with that path, but we can at least
	 * make sure the attribute is supported.
	 */
	@Test
	fun rollbackWitPath() {
		buildChangeSet {
			addColumn(tableName = "monkey") {
				column(name = "diet", type = "varchar(30)")
			}
			rollback(changeSetId = CHANGESET_ID, changeSetAuthor = CHANGESET_AUTHOR, changeSetPath = CHANGESET_FILEPATH)
		}

		// in this case, we expect the addColumn change to also be the change
		// inside the rollback.
		Assert.assertEquals(1, changeSet.changes.size)
		val changes = changeSet.rollback.changes
		assertNotNull(changes)
		Assert.assertEquals(1, changes.size)
		assertTrue(changes[0] is AddColumnChange)
		assertEquals("monkey", (changes[0] as AddColumnChange).tableName)
		assertNoOutput()
	}
}

