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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for {@link ArgumentDelegate}  It makes sure it can be called in all
 * its various permutations.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ArgsTests {

	/**
	 * Test what happens when the closure is empty.  This is fine, and we should
	 * have no arguments when we're done.
	 */
	@Test
	fun emptyArguments() {
		val args = buildArguments {}

		assertNotNull(args)
		assertEquals(0, args.size)
	}

	/**
	 * Test what happens when we have a single argument that is a string.
	 */
	@Test
	fun oneStringArgument() {
		val args = buildArguments {
			arg("one")
		}

		assertNotNull(args)
		assertEquals(1, args.size)
		assertEquals("one", args[0])
	}

	/**
	 * Test what happens when we have a two arguments that is are strings.
	 */
	@Test
	fun twoStringArguments() {
		val args = buildArguments {
			arg("one")
			arg("two")
		}

		assertNotNull(args)
		assertEquals(2, args.size)
		assertEquals("one", args[0])
		assertEquals("two", args[1])
	}

	/**
	 * Helper method to execute an {@link ArgumentDelegate} and return any
	 * arguments it created.
	 * @param closure
	 * @return
	 */
	private fun buildArguments(closure: (KotlinArgs).() -> Unit) : List<String> {
		val changeLog = DatabaseChangeLog()
		changeLog.changeLogParameters = ChangeLogParameters()
		
		val kotlinArgs = KotlinArgs(changeLog)
		kotlinArgs.closure()

		return kotlinArgs.args
	}
}
