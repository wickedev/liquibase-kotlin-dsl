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
import liquibase.sql.visitor.*
import org.junit.Test
import org.liquibase.kotlin.helper.assertType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test class for the {@link ModifySqlDelegate}.  As usual, we're only verifying
 * that we can pass things to Liquibase correctly. We check all attributes that
 * are known at this time - note that several are undocumented.  Thia
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class ModifySqlDelegateTests {

	/**
	 * Test the modifySql delegate with empty attributes and an empty closure.
	 */
	@Test
	fun modifySqlEmpty() {
		val sqlVisitors = buildDelegate {}
		assertEquals(0, sqlVisitors.size)
	}

	/**
	 * Test the append element with no attributes. This time, we'll also make sure
	 * we can set applyToRollback to true.
	 */
	@Test
	fun appendEmpty() {
		val sqlVisitors = buildDelegate(applyToRollback = true) {
			append(value = "")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<AppendSqlVisitor>(sqlVisitors[0]) {
			assertEquals("", it.value)
			assertNull(it.applicableDbms)
			assertNull(it.contexts)
			assertNull(it.labels)
			assertTrue(it.isApplyToRollback)
		}

	}

	/**
	 * Test the append element with all supported attributes.  For this test,
	 * we will also set a context and a label
	 */
	@Test
	fun appendFull() {
		val sqlVisitors = buildDelegate(context = "test", labels = "test_label") {
			append(value = "exec")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<AppendSqlVisitor>(sqlVisitors[0]) {
			assertEquals("exec", it.value)
			assertNull(it.applicableDbms)
			assertEquals("test", it.contexts.contexts.first())
			assertEquals("test_label", it.labels.toString())
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * Test the prepend element with no attributes. Validate the default
	 * "applyToRollback" value of false.
	 */
	@Test
	fun prependEmpty() {
		val sqlVisitors = buildDelegate {
			prepend(value = "")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<PrependSqlVisitor>(sqlVisitors[0]) {
			assertEquals("", it.value)
			assertNull(it.applicableDbms)
			assertNull(it.contexts)
			assertNull(it.labels)
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * Test the prepend element with all supported attributes.  For this test,
	 * we will also set a dbms.
	 */
	@Test
	fun prependFull() {
		val sqlVisitors = buildDelegate(dbms = "mysql") {
			prepend(value = "exec")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<PrependSqlVisitor>(sqlVisitors[0]) {
			assertEquals("exec", it.value)
			assertEquals("mysql", it.applicableDbms.first())
			assertNull(it.contexts)
			assertNull(it.labels)
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * Test the replace element with no attributes. This time, we'll try to
	 * explicitly set applyToRollback to false.
	 */
	@Test
	fun replaceEmpty() {
		val sqlVisitors = buildDelegate(applyToRollback = false) {
			replace(replace = "", with = "")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<ReplaceSqlVisitor>(sqlVisitors[0]) {
			assertEquals("", it.replace)
			assertEquals("", it.with)
			assertNull(it.applicableDbms)
			assertNull(it.contexts)
			assertNull(it.labels)
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * Test the replace element with all supported attributes.  For this test,
	 * we will also set a 2 databases to test that it gets split correctly.
	 */
	@Test
	fun replaceFull() {
		val sqlVisitors = buildDelegate(dbms = "mysql,oracle") {
			replace(replace = "execute", with = "exec")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<ReplaceSqlVisitor>(sqlVisitors[0]) {
			assertEquals("execute", it.replace)
			assertEquals("exec", it.with)
			assertEquals(2, it.applicableDbms.size)
			assertTrue(it.applicableDbms.contains("mysql"))
			assertTrue(it.applicableDbms.contains("oracle"))
			assertNull(it.contexts)
			assertNull(it.labels)
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * Test the regExpReplace element with no attributes. This time, we'll also make sure
	 * we can set applyToRollback to the string "true".
	 */
	@Test
	fun regExpReplaceEmpty() {
		val sqlVisitors = buildDelegate(applyToRollback = "true") {
			regExpReplace(replace = "", with = "")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<RegExpReplaceSqlVisitor>(sqlVisitors[0]) {
			assertEquals("", it.replace)
			assertEquals("", it.with)
			assertNull(it.applicableDbms)
			assertNull(it.contexts)
			assertNull(it.labels)
			assertTrue(it.isApplyToRollback)
		}
	}

	/**
	 * Test the regExpReplace element with all supported attributes.  For this test,
	 * we will also set two contexts.
	 */
	@Test
	fun regExpReplaceFull() {
		val sqlVisitors = buildDelegate(context = "test,ci") {
			regExpReplace(replace = "execute", with = "exec")
		}
		assertEquals(1, sqlVisitors.size)
		assertType<RegExpReplaceSqlVisitor>(sqlVisitors[0]) {
			assertEquals("execute", it.replace)
			assertEquals("exec", it.with)
			assertNull(it.applicableDbms)
			assertEquals(2, it.contexts.contexts.size)
			assertTrue(it.contexts.contexts.contains("test"))
			assertTrue(it.contexts.contexts.contains("ci"))
			assertNull(it.labels)
			assertFalse(it.isApplyToRollback)
		}
	}

	/**
	 * helper method to build and execute a ModifySqlDelegate.
	 * @param params the parameters to pass to the new delegate
	 * @param closure the closure to execute
	 * @return the new delegate.
	 */
	private fun buildDelegate(dbms: String? = null, context: String? = null, applyToRollback: Any? = null,
							  labels: String? = null, closure: (KotlinModifySql).() -> Unit) : List<SqlVisitor> {
		val changeLog = DatabaseChangeLog()
		changeLog.changeLogParameters = ChangeLogParameters()
		
		val delegate = KotlinModifySql(dbms, context, labels, applyToRollback, changeLog)
		delegate.closure()

		return delegate.sqlVisitors
	}
}

