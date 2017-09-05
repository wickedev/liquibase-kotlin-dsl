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

import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import liquibase.precondition.CustomPreconditionWrapper
import liquibase.precondition.Precondition
import liquibase.precondition.core.*
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * This class tests the creation of Liquibase ChangeSet Preconditions.  It is
 * probably a bit of overkill since most preconditions are set by passing
 * named preconditions to the Liquibase factory, but it does serve to make sure
 * that ll the preconditions currently known will work as we would expect.
 * <p>
 * since we just pass through to Liquibase, we're not too concerned with
 * validating attrubutes.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class PreconditionTests {

	/**
	 * Try creating a dbms precondition
	 */
	@Test
	fun dbmsPrecondition() {
		val preconditions = buildPreconditions {
			dbms(type = "mysql")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<DBMSPrecondition>(preconditions[0]) {
			assertEquals("mysql", it.type)
		}
	}

	/**
	 * Try creating a runningAs precondition.
	 */
	@Test
	fun runningAsPrecondition() {
		val preconditions = buildPreconditions {
			runningAs(username = "tlberglund")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<RunningAsPrecondition>(preconditions[0]) {
			assertEquals("tlberglund", it.username)
		}
	}

	/**
	 * Try creating a changeSetExecuted precondition.
	 */
	@Test
	fun changeSetExecutedPrecondition() {
		val preconditions = buildPreconditions {
			changeSetExecuted(id = "unleash-monkey", author = "tlberglund", changeLogFile = "changelog.xml")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<ChangeSetExecutedPrecondition>(preconditions[0]) {
			assertEquals("unleash-monkey", it.id)
			assertEquals("tlberglund", it.author)
			assertEquals("changelog.xml", it.changeLogFile)
		}
	}

	/**
	 * Try creating a columnExists precondition.
	 */
	@Test
	fun columnExistsPrecondition() {
		val preconditions = buildPreconditions {
			columnExists(schemaName = "schema", tableName = "monkey", columnName = "emotion")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<ColumnExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
			assertEquals("emotion", it.columnName)
		}
	}

	/**
	 * try creating a tableExists precondition.
	 */
	@Test
	fun tableExistsPrecondition() {
		val preconditions = buildPreconditions {
			tableExists(schemaName = "schema", tableName = "monkey")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<TableExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("monkey", it.tableName)
		}
	}

	/**
	 * Try creating a vewExists precondition.
	 */
	@Test
	fun viewExistsPrecondition() {
		val preconditions = buildPreconditions {
			viewExists(schemaName = "schema", viewName = "monkey_view")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<ViewExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("monkey_view", it.viewName)
		}
	}

	/**
	 * Try creating a foreignKeyConstraintExists precondition
	 */
	@Test
	fun foreignKeyConstraintExistsPrecondition() {
		val preconditions = buildPreconditions {
			foreignKeyConstraintExists(schemaName = "schema", foreignKeyName = "fk_monkey_key")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<ForeignKeyExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("fk_monkey_key", it.foreignKeyName)
		}
	}

	/**
	 * Try creating an indexExists precondition.
	 */
	@Test
	fun indexExistsPrecondition() {
		val preconditions = buildPreconditions {
			indexExists(schemaName = "schema", indexName = "index")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<IndexExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("index", it.indexName)
		}
	}

	/**
	 * Try creating a sequenceExists precondition.
	 */
	@Test
	fun sequenceExistsPrecondition() {
		val preconditions = buildPreconditions {
			sequenceExists(schemaName = "schema", sequenceName = "seq_next_monkey")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<SequenceExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("seq_next_monkey", it.sequenceName)
		}
	}

	/**
	 * Try creating a primaryKeyExists precondition.
	 */
	@Test
	fun primaryKeyExistsPrecondition() {
		val preconditions = buildPreconditions {
			primaryKeyExists(schemaName = "schema", primaryKeyName = "pk_monkey")
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<PrimaryKeyExistsPrecondition>(preconditions[0]) {
			assertEquals("schema", it.schemaName)
			assertEquals("pk_monkey", it.primaryKeyName)
		}
	}

	/**
	 * And clauses are handled a little differently. Make sure we can create it
	 * correctly.
	 */
	@Test
	fun andClause() {
		val preconditions = buildPreconditions {
			and {
				dbms(type = "mysql")
				runningAs(username = "tlberglund")
			}
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<AndPrecondition>(preconditions[0]) {
			val andedPreconditions = it.nestedPreconditions
			assertNotNull(andedPreconditions)
			assertEquals(2, andedPreconditions.size)
			assertTrue(andedPreconditions[0] is DBMSPrecondition)
			assertTrue(andedPreconditions[1] is RunningAsPrecondition)
		}
	}

	/**
	 * Or clauses are handled a little differently. Make sure we can create it
	 * correctly.
	 */
	@Test
	fun orClause() {
		val preconditions = buildPreconditions {
			or {
				dbms(type = "mysql")
				runningAs(username = "tlberglund")
			}
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertTrue(preconditions[0] is OrPrecondition)
		assertType<OrPrecondition>(preconditions[0]) {
			val oredPreconditions = it.nestedPreconditions
			assertNotNull(oredPreconditions)
			assertEquals(2, oredPreconditions.size)
			assertTrue(oredPreconditions[0] is DBMSPrecondition)
			assertTrue(oredPreconditions[1] is RunningAsPrecondition)
		}
	}

	/**
	 * Not clauses are handled a little differently. Make sure we can create it
	 * correctly.
	 */
	@Test
	fun notClause() {
		val preconditions = buildPreconditions {
			not {
				dbms(type = "mysql")
				runningAs(username = "tlberglund")
			}
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<NotPrecondition>(preconditions[0]) {
			val notedPreconditions = it.nestedPreconditions
			assertNotNull(notedPreconditions)
			assertEquals(2, notedPreconditions.size)
			assertTrue(notedPreconditions[0] is DBMSPrecondition)
			assertTrue(notedPreconditions[1] is RunningAsPrecondition)
		}
	}

	/**
	 * Try creating a sqlCheck precondition with all currently known attributes
	 * and some SQL in the closure.
	 */
	@Test
	fun sqlCheckFull() {
		val preconditions = buildPreconditions {
			sqlCheck(expectedResult = "angry") {
				"SELECT emotion FROM monkey WHERE id=2884"
			}
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<SqlPrecondition>(preconditions[0]) {
			assertEquals("angry", it.expectedResult)
			assertEquals("SELECT emotion FROM monkey WHERE id=2884", it.sql)
		}
	}

	/**
	 * Try creating a custom precondition with 2 nested param elements.
	 */
	@Test
	fun customPreconditionTwoParamElements() {
		val preconditions = buildPreconditions {
			customPrecondition(className = "org.liquibase.precondition.MonkeyFailPrecondition") {
				param(name = "emotion", value = "angry")
				param(name = "rfid-tag", value = 28763)
			}
		}

		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertType<CustomPreconditionWrapper>(preconditions[0]) {
			assertEquals("angry", it.getParamValue("emotion"))
			assertEquals("28763", it.getParamValue("rfid-tag")) // Liquibase converts to string.
		}
	}

	/**
	 * Helper method to run the precondition closure and return the preconditions.
	 * @param closure the closure to call
	 * @return the preconditions that were created.
	 */
	private fun buildPreconditions(closure: (KotlinPrecondition).() -> Unit): List<Precondition> {
		val changelog = DatabaseChangeLog()
		changelog.changeLogParameters = ChangeLogParameters()

		val delegate = KotlinPrecondition(null, null, null, null, null, changelog)
		delegate.closure()
		
		changelog.preconditions = delegate.preconditions
		
		return changelog.preconditions.nestedPreconditions
	}
}

