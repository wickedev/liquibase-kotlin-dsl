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

package org.liquibase.groovy.delegate

import liquibase.Scope
import liquibase.changelog.ChangeLog
import liquibase.database.DatabaseFactory
import liquibase.parser.ParsedNode
import liquibase.parser.mapping.ParsedNodeMappingFactory
import org.junit.After
import org.junit.Before
import org.liquibase.groovy.helper.JUnitResourceAccessor

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static org.junit.Assert.assertTrue

/**
 * This is the base class for all of the change set related tests.  It mostly
 * contains utility methods to help with testing.
 *
 * @author Steven C. Saliman
 */
class ChangeSetTests {
	def CHANGESET_ID = 'generic-changeset-id'
	def CHANGESET_AUTHOR = 'ssaliman'
	def CHANGESET_FILEPATH = '/filePath'
	def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	def changeSet
	def resourceAccessor
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
	 * Helper method that builds a changeSet from the given closure.  Tests will
	 * use this to test parsing the various closures that make up the Groovy DSL.
	 * @param closure the closure containing changes to parse.
	 * @return the changeSet, with parsed changes from the closure added.
	 */
	def buildChangeSet(parentNodeName, returnType, Closure closure) {
		def parentNode
		if ( parentNodeName != null ) {
			parentNode = ParsedNode.createRootNode(parentNodeName)
		}
//		changelog.changeLogParameters = new ChangeLogParameters()
//		changelog.changeLogParameters.set('database.typeName', 'mysql')

		closure.delegate = new DatabaseChangeLogDelegate(parentNode)
		closure.call()
		parentNode = closure.delegate.parentNode
		def url = new File(System.getProperty("java.io.tmpdir")).toURI().toURL()
		def urls = [url] as URL[]
		Scope scope = new Scope(new JUnitResourceAccessor(urls), [:])
		scope.getSingleton(ParsedNodeMappingFactory.class).toObject(parentNode, returnType, null, null, scope);


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

