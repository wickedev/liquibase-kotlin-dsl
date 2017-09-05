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

import liquibase.change.ConstraintsConfig
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

/**
 * This class tests that a constraint closure can be parsed correctly by the
 * Kotlin DSL.  As with the other tests, we are not interested in whether or
 * not we end up with a valid constraint, just whether or not we faithfully
 * created a Liquibase object from the Kotlin closure we were given.  We
 * defer to Liquibase on matters of validity.  Once again, Liquibase has
 * some options that are not documented, so we'll test them.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ConstraintTests {
	private val oldStdOut = System.out
	private val bufStr = ByteArrayOutputStream()

	/**
	 * Set up for each test by and capturing standard out so that tests can check
	 * for the presence/absence of messages.
	 */
	@Before
	fun captureStdOut() {
		System.setOut(PrintStream(bufStr))
	}

	/**
	 * After each test, make sure stdout is back to what it should be, and for
	 * good measure, print out any output we got.
	 */
	@After
	fun restoreStdOut() {
		if (oldStdOut != null) {
			System.setOut(oldStdOut)
		}
		val testOutput = bufStr.toString()
		if (testOutput.isNotEmpty()) {
			println("Test output:\n$testOutput")
		}
	}

	/**
	 * Test parsing constraints when we have no args and no closure.  This
	 * validates that we don't set any unintended defaults.
	 */
	@Test
	fun constraintWithoutArgsOrClosure() {
		val constraint = buildConstraint {
			constraints()
		}

		assertNotNull(constraint)
		assertNull(constraint.isNullable)
		assertNull(constraint.isPrimaryKey)
		assertNull(constraint.primaryKeyName)
		assertNull(constraint.primaryKeyTablespace)
		assertNull(constraint.foreignKeyName)
		assertNull(constraint.references)
		assertNull(constraint.referencedTableName)
		assertNull(constraint.referencedColumnNames)
		assertNull(constraint.isUnique)
		assertNull(constraint.uniqueConstraintName)
		assertNull(constraint.checkConstraint)
		assertNull(constraint.isDeleteCascade)
		assertNull(constraint.isInitiallyDeferred)
		assertNull(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Test parsing constraints when we set all the attributes via the argument
	 * map.  We have too many booleans to isolate them, so we'll need some more
	 * tests there...
	 */
	@Test
	fun fullArgumentsNoClosure() {
		val constraint = buildConstraint {
			constraints(
					nullable = true,
					primaryKey = true,
					primaryKeyName = "myPrimaryKey",
					primaryKeyTablespace = "myPrimaryTablespace",
					foreignKeyName = "fk_monkey",
					references = "monkey(id)",
					referencedTableName = "monkey",
					referencedColumnNames = "id",
					unique = true,
					uniqueConstraintName = "myUniqueKey",
					checkConstraint = "myCheckConstraint",
					deleteCascade = true,
					initiallyDeferred = true,
					deferrable = true
			)
		}

		assertNotNull(constraint)
		assertTrue(constraint.isNullable)
		assertTrue(constraint.isPrimaryKey)
		assertEquals("myPrimaryKey", constraint.primaryKeyName)
		assertEquals("myPrimaryTablespace", constraint.primaryKeyTablespace)
		assertEquals("fk_monkey", constraint.foreignKeyName)
		assertEquals("monkey(id)", constraint.references)
		assertEquals("monkey", constraint.referencedTableName)
		assertEquals("id", constraint.referencedColumnNames)
		assertTrue(constraint.isUnique)
		assertEquals("myUniqueKey", constraint.uniqueConstraintName)
		assertEquals("myCheckConstraint", constraint.checkConstraint)
		assertTrue(constraint.isDeleteCascade)
		assertTrue(constraint.isInitiallyDeferred)
		assertTrue(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "nullable" is true.
	 */
	@Test
	fun onlyNullableArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = true,
					primaryKey = false,
					unique = false,
					deleteCascade = false,
					initiallyDeferred = false,
					deferrable = false
			)
		}

		assertNotNull(constraint)
		assertTrue(constraint.isNullable)
		assertFalse(constraint.isPrimaryKey)
		assertFalse(constraint.isUnique)
		assertFalse(constraint.isDeleteCascade)
		assertFalse(constraint.isInitiallyDeferred)
		assertFalse(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "primaryKey" is true.
	 */
	@Test
	fun onlyPrimaryKeyArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = false,
					primaryKey = true,
					unique = false,
					deleteCascade = false,
					initiallyDeferred = false,
					deferrable = false
			)
		}

		assertNotNull(constraint)
		assertFalse(constraint.isNullable)
		assertTrue(constraint.isPrimaryKey)
		assertFalse(constraint.isUnique)
		assertFalse(constraint.isDeleteCascade)
		assertFalse(constraint.isInitiallyDeferred)
		assertFalse(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "unique" is true.
	 */
	@Test
	fun onlyUniqueArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = false,
					primaryKey = false,
					unique = true,
					deleteCascade = false,
					initiallyDeferred = false,
					deferrable = false
			)
		}

		assertNotNull(constraint)
		assertFalse(constraint.isNullable)
		assertFalse(constraint.isPrimaryKey)
		assertTrue(constraint.isUnique)
		assertFalse(constraint.isDeleteCascade)
		assertFalse(constraint.isInitiallyDeferred)
		assertFalse(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "deleteCascade" is true.
	 */
	@Test
	fun onlyDeleteCascadeArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = false,
					primaryKey = false,
					unique = false,
					deleteCascade = true,
					initiallyDeferred = false,
					deferrable = false
			)
		}

		assertNotNull(constraint)
		assertFalse(constraint.isNullable)
		assertFalse(constraint.isPrimaryKey)
		assertFalse(constraint.isUnique)
		assertTrue(constraint.isDeleteCascade)
		assertFalse(constraint.isInitiallyDeferred)
		assertFalse(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "initiallyDeferred"
	 * is true.
	 */
	@Test
	fun onlyInitiallyDeferredArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = false,
					primaryKey = false,
					unique = false,
					deleteCascade = false,
					initiallyDeferred = true,
					deferrable = false
			)
		}

		assertNotNull(constraint)
		assertFalse(constraint.isNullable)
		assertFalse(constraint.isPrimaryKey)
		assertFalse(constraint.isUnique)
		assertFalse(constraint.isDeleteCascade)
		assertTrue(constraint.isInitiallyDeferred)
		assertFalse(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Set all the boolean attributes via arguments, but only "deferrable" is true.
	 */
	@Test
	fun onlyDeferrableArgIsTrue() {
		val constraint = buildConstraint {
			constraints(
					nullable = false,
					primaryKey = false,
					unique = false,
					deleteCascade = false,
					initiallyDeferred = false,
					deferrable = true
			)
		}

		assertNotNull(constraint)
		assertFalse(constraint.isNullable)
		assertFalse(constraint.isPrimaryKey)
		assertFalse(constraint.isUnique)
		assertFalse(constraint.isDeleteCascade)
		assertFalse(constraint.isInitiallyDeferred)
		assertTrue(constraint.isDeferrable)

		assertNoOutput()
	}

	/**
	 * Constraints have an interesting wrinkle.  Where multiple calls to "column"
	 * results in multiple columns being created, multiple calls to "constraint"
	 * results in just one, combined constraint.  This is useful if a column
	 * has both a unique constraint and a foreign key constraint.  Test this.
	 */
	@Test
	fun constraintsFromMapWithMultipleCalls() {
		val constraint = buildConstraint {
			constraints(foreignKeyName = "fk_monkey", references = "monkey(id)")
			constraints(unique = true, uniqueConstraintName = "uk_monkey")
		}

		assertNotNull(constraint)
		assertEquals("fk_monkey", constraint.foreignKeyName)
		assertEquals("monkey(id)", constraint.references)
		assertTrue(constraint.isUnique)
		assertEquals("uk_monkey", constraint.uniqueConstraintName)

		assertNoOutput()
	}

	/**
	 * Make sure the test did not have any output to standard out.  This can be
	 * used to make sure there are no deprecation warnings.
	 */
	private fun assertNoOutput() {
		val testOutput = bufStr.toString()
		assertTrue(testOutput.isEmpty(), "Did not expect to have output, but got:\n$testOutput")
	}

	/**
	 * Helper method to execute a constraint closure and return the constraint
	 * created from it.
	 * @param closure the closure to execute
	 * @return the closure object built.
	 */
	private fun buildConstraint(closure: (KotlinColumnConstraint).() -> Unit) : ConstraintsConfig {
		val databaseChangeLog = DatabaseChangeLog().apply {
			changeLogParameters = ChangeLogParameters()
		}

		val constraint = KotlinColumnConstraint(databaseChangeLog)
		constraint.closure()
		return constraint.constraint
	}

}
