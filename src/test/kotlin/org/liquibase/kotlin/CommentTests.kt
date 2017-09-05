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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for {@link CommentDelegate}  It makes sure it can be called in all
 * its various permutations.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class CommentTests : ChangeSetTests() {

	/**
	 * Test what happens when the closure is empty.  This is fine, and we should
	 * have no comments when we're done.
	 */
	@Test
	fun emptyComment() {
		buildChangeSet { }
		assertNull(changeSet.comments)
	}

	/**
	 * Test what happens when we have a comment and no SQL..
	 */
	@Test
	fun commentsNoSql() {
		buildChangeSet {
			comment("No comment")
		}

		assertEquals("No comment", changeSet.comments)
	}

	/**
	 * Test what happens when we have a two comments, and some SQL.  In this case
	 * the comments should be appended.  We'll also add some Sql to the mix.
	 */
	@Test
	fun twoCommentsWithSql() {
		buildChangeSet {
			comment("first")
			comment {
				"second"
			}
		}

		assertEquals("first second", changeSet.comments)
	}
}
