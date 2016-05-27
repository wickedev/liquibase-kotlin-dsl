/*
 * Copyright 2011-2016 Steven C. Saliman
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

package liquibase.parser.groovy.delegate

import liquibase.JUnitScope
import liquibase.Scope
import liquibase.changelog.ChangeLog
import liquibase.parser.ParserFactory
import liquibase.resource.MockResourceAccessor
import org.junit.After
import org.junit.Before

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.*

/**
 * This is the base class for all of the integration tests.  It contains the
 * code that does the heavy lifting of talking to Liquibase and returning the
 * desired objects so that individual tests can focus only on the parts that
 * are being examined by the test in question.
 * <p>
 * With the new ParsedNode structure in Liquibase, there is not much to test
 * at a unit test level, so the bulk of the tests in the Groovy DSL are
 * integration tests to make sure that we get the expected Liquibase objects
 * when we parse any given part of the DSL.
 *
 * @author Steven C. Saliman
 */
class IntegrationTest {
	def CHANGESET_ID = 'generic-changeset-id'
	def CHANGESET_AUTHOR = 'ssaliman'
	def CHANGESET_FILEPATH = '/filePath'
	def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	def oldStdOut = System.out;
	def bufStr = new ByteArrayOutputStream()

	/**
	 * Set up for each test.  This involves two things; creating a change set
	 * for each test to modify with changes, and capture stdout so that tests
	 * can check for the presence/absence of messages.
	 */
	@Before
	void createChangeSet() {
		// Capture stdout to confirm the presence of a deprecation warning.
		System.out = new PrintStream(bufStr)

	}

	/**
	 * After each test, make sure stdout is back to what it should be, and for
	 * good measure, print out any output we got.
	 */
	@After
	void restoreStdOut() {
		if ( oldStdOut != null ) {
			System.out = oldStdOut
		}
		String testOutput = bufStr.toString()
		if ( testOutput != null && testOutput.length() > 0 ) {
			println("Test output:\n${testOutput}")
		}
	}

	/**
	 * Parse the given changeLog and return a Liquibase ChangeLog object.
	 * @param changeLogString the string representation of the groovy changeLog
	 * closure to parse.
	 * @return The Liquibase ChangeLog object created when Liquibase uses the
	 * Groovy parser to parse the given ChangeLog.
	 */
	def parseChangeLog(changeLogString) {
		def path = "test-changelog.groovy"
		def mockAccessor = new MockResourceAccessor()
		mockAccessor.addData(path, changeLogString)

		def scope = JUnitScope.instance.child(Scope.Attr.resourceAccessor, mockAccessor)
		def changeLog = scope.getSingleton(ParserFactory.class).parse(path, ChangeLog.class, scope);
		assertNotNull changeLog
		return changeLog
	}

	/**
	 * Parse the given changeSet and return a liquibase ChangeSet object.
	 * <p>
	 * This method will wrap the given ChangeSet string in a valid
	 * databaseChangeLog element and have Liquibase parse the whole thing.  The
	 * first ChangeSet found in the resulting parse run will be returned.
	 * @param changeLogString the string representation of the groovy changeSet
	 * closure to parse.
	 * @return the first ChangeSet created by Liquibase when it uses the Groovy
	 * parser to parse the changeLog containing our changeSet.
	 */
	def parseChangeSet(changeSetString) {
		def changeLogString = "databaseChangeLog {\n ${changeSetString}\n}"
		def changeLog = parseChangeLog(changeLogString)
		assertNotNull changeLog
		assertNotNull changeLog.changeSets
		assertEquals 1, changeLog.changeSets.size()
		// TODO: Should we look for rollback = 0 here?
		return changeLog.changeSets[0]
	}

	/**
	 * Parse the given action and return a Liquibase Action class.
	 * <p>
	 * This method will take the given action closure (createTable, dropIndex,
	 * etc), and wrap it in a valid changeSet for parsing.  It will have
	 * Liquibase parse the whole thing and return the first Action class in the
	 * returned results.
	 * @param actionString the string representation of the groovy action
	 * closure to parse.
	 * @return the first Action created by Liquibase when it uses the Groovy
	 * parser to parse the changeLog containing our action.
	 */
	def parseAction(actionString) {
		def changeSetString = "changeSet (id: 'test', author: 'steve') {\n${actionString}\n}"
		def changeSet = parseChangeSet(changeSetString)
		assertNotNull changeSet
		assertNotNull changeSet.actions[0]
		assertEquals 1, changeSet.actions.size()
		return changeSet.actions[0]
	}

	/**
	 * Small helper to parse a string into a Timestamp
	 * @param dateTimeString the string to parse
	 * @return the parsed string
	 */
	Timestamp parseSqlTimestamp(dateTimeString) {
		new Timestamp(sdf.parse(dateTimeString).time)
	}

	/**
	 * Make sure the given message is present in the standard output.  This can
	 * be used to verify that we got expected deprecation warnings.  This method
	 * will fail the test of the given message is not in standard out.
	 * @param message the message that must exist.
	 */
	def assertPrinted(message) {
		String testOutput = bufStr.toString()
		assertTrue "'${message}' was not found in:\n '${testOutput}'",
				testOutput.contains(message)
	}

	/**
	 * Make sure the test did not have any output to standard out.  This can be
	 * used to make sure there are no deprecation warnings.
	 */
	def assertNoOutput() {
		String testOutput = bufStr.toString()
		assertTrue "Did not expect to have output, but got:\n '${testOutput}",
				testOutput.length() < 1
	}
}

