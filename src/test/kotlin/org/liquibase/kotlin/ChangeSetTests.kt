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
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.resource.FileSystemResourceAccessor
import org.junit.After
import org.junit.Before
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.sql.Timestamp
import java.text.SimpleDateFormat
import kotlin.test.assertTrue


/**
 * This is the base class for all of the change set related tests.  It mostly
 * contains utility methods to help with testing.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
open class ChangeSetTests {
	val CHANGESET_ID = "generic-changeset-id"
	val CHANGESET_AUTHOR = "tlberglund"
	val CHANGESET_FILEPATH = "/filePath"
	private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	private lateinit var kotlinChangeSet: KotlinChangeSet
	private lateinit var changeLog: DatabaseChangeLog
	private val oldStdOut = System.out
	private val bufStr = ByteArrayOutputStream()

	val changeSet: ChangeSet
		get() = kotlinChangeSet.changeSet

	/**
	 * Set up for each test.  This involves two things; creating a change set
	 * for each test to modify with changes, and capture stdout so that tests
	 * can check for the presence/absence of messages.
	 */
	@Before
	fun createChangeSet() {
		changeLog = DatabaseChangeLog(CHANGESET_FILEPATH)
		changeLog.changeLogParameters = ChangeLogParameters()
		changeLog.changeLogParameters.set("database.typeName", "mysql")
		kotlinChangeSet = KotlinChangeSet(
				CHANGESET_ID,
				CHANGESET_AUTHOR,
				false,
				false,
				"context",
				"mysql",
				null,
				true,
				changeLog,
				true,
				null,
				null,
				true)

		kotlinChangeSet.resourceAccessor = FileSystemResourceAccessor()

		// Capture stdout to confirm the presence of a deprecation warning.
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
	 * Helper method that builds a changeSet from the given closure.  Tests will
	 * use this to test parsing the various closures that make up the Kotlin DSL.
	 * @param closure the closure containing changes to parse.
	 * @return the changeSet, with parsed changes from the closure added.
	 */
	fun buildChangeSet(closure: (KotlinChangeSet).() -> Unit) {
		kotlinChangeSet.closure()
	}

	/**
	 * Small helper to parse a string into a Timestamp
	 * @param dateTimeString the string to parse
	 * @return the parsed string
	 */
	fun parseSqlTimestamp(dateTimeString: String): Timestamp {
		return Timestamp(sdf.parse(dateTimeString).time)
	}

	/**
	 * Make sure the test did not have any output to standard out.  This can be
	 * used to make sure there are no deprecation warnings.
	 */
	fun assertNoOutput() {
		val testOutput = bufStr.toString()
		assertTrue(testOutput.isEmpty(), "Did not expect to have output, but got:\n \"$testOutput\"")
	}
}

