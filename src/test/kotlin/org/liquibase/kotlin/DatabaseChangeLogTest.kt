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

import liquibase.parser.ext.KotlinLiquibaseChangeLogParser
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.*

/**
 * Test class for the {@link KotlinDatabaseChangeLog}
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class DatabaseChangeLogTest {
	companion object {
		private const val FILE_PATH = "src/test/resources"
		private val TMP_CHANGELOG_DIR = File("$FILE_PATH/tmp")
		private val TMP_INCLUDE_DIR = File("$TMP_CHANGELOG_DIR/include")
		private const val EMPTY_CHANGELOG = "$FILE_PATH/empty-changelog.kts"
		private const val SIMPLE_CHANGELOG = "$FILE_PATH/simple-changelog.kts"
		private const val ROOT_CHANGE_SET = "root-change-set"
		private const val FIRST_INCLUDED_CHANGE_SET = "included-change-set-1"
		private const val SECOND_INCLUDED_CHANGE_SET = "included-change-set-2"
	}

	private lateinit var resourceAccessor: ResourceAccessor
	private lateinit var parserFactory: ChangeLogParserFactory


	@Before
	fun registerParser() {
		resourceAccessor = FileSystemResourceAccessor(".")
		parserFactory = ChangeLogParserFactory.getInstance()
		ChangeLogParserFactory.getInstance().register(KotlinLiquibaseChangeLogParser())
		// make sure we start with clean temporary directories before each test
		TMP_CHANGELOG_DIR.deleteRecursively()
		TMP_INCLUDE_DIR.mkdirs()
	}

	/**
	 * Attempt to clean up included files and directories.  We do this every time
	 * to make sure we start clean each time.  The includeAll test depends on it.
	 */
	@After
	fun cleanUp() {
		TMP_CHANGELOG_DIR.deleteRecursively()
	}

	@Test
	fun parseEmptyChangelog() {
		val parser = parserFactory.getParser(EMPTY_CHANGELOG, resourceAccessor)

		assertNotNull(parser, "Kotlin changelog parser was not found")

		val changeLog = parser.parse(EMPTY_CHANGELOG, ChangeLogParameters(), resourceAccessor)
		assertNotNull(changeLog, "Parsed DatabaseChangeLog was null")
		assertTrue(changeLog is DatabaseChangeLog, "Parser result was not a DatabaseChangeLog")
	}

	@Test
	fun parseSimpleChangelog() {
		val parser = parserFactory.getParser(SIMPLE_CHANGELOG, resourceAccessor)

		assertNotNull(parser, "Kotlin changelog parser was not found")

		val changeLog = parser.parse(SIMPLE_CHANGELOG, ChangeLogParameters(), resourceAccessor)
		assertNotNull(changeLog, "Parsed DatabaseChangeLog was null")
		assertTrue(changeLog is DatabaseChangeLog, "Parser result was not a DatabaseChangeLog")
		assertEquals(".", changeLog.logicalFilePath)

		val changeSets = changeLog.changeSets
		assertEquals(1, changeSets.size)
		val changeSet = changeSets[0]
		assertNotNull(changeSet, "ChangeSet was null")
		assertEquals("Jason Blackwell", changeSet.author)
		assertEquals("1", changeSet.id)
	}

	@Test
	fun preconditionParameters() {
		val closure: (KotlinDatabaseChangeLog).() -> Unit = {
			preConditions(
					onFail = PreconditionContainer.FailOption.WARN,
					onError = PreconditionContainer.ErrorOption.MARK_RAN,
					onUpdateSQL = PreconditionContainer.OnSqlOutputOption.TEST,
					onFailMessage = "fail-message!!!1!!1one!",
					onErrorMessage = "error-message")
		}

		val databaseChangeLog = DatabaseChangeLog("changelog.xml")
	    databaseChangeLog.changeLogParameters = ChangeLogParameters()
		val delegate = KotlinDatabaseChangeLog(databaseChangeLog)
		delegate.closure()

		val preconditions = databaseChangeLog.preconditions
		assertNotNull(preconditions)
		assertTrue(preconditions is PreconditionContainer)
		assertEquals(PreconditionContainer.FailOption.WARN, preconditions.onFail)
		assertEquals(PreconditionContainer.ErrorOption.MARK_RAN, preconditions.onError)
		assertEquals(PreconditionContainer.OnSqlOutputOption.TEST, preconditions.onSqlOutput)
		assertEquals("fail-message!!!1!!1one!", preconditions.onFailMessage)
		assertEquals("error-message", preconditions.onErrorMessage)
	}


	/**
	 * Test creating a changeSet with no attributes. This verifies that we use
	 * expected valault values when a value is not provided.
	 */
	@Test
	fun changeSetEmpty() {
		val changeLog = buildChangeLog {
			changeSet(id = "1", author = "Test")
		}

		assertNotNull(changeLog.changeSets)
		assertEquals(1, changeLog.changeSets.size)
		assertEquals("1", changeLog.changeSets[0].id)
		assertEquals("Test", changeLog.changeSets[0].author)
		assertFalse(changeLog.changeSets[0].isAlwaysRun)
		// the property doesn"t match xml or docs.
		assertFalse(changeLog.changeSets[0].isRunOnChange)
		assertEquals(FILE_PATH, changeLog.changeSets[0].filePath)
		assertEquals(0, changeLog.changeSets[0].contexts.contexts.size)
		assertNull(changeLog.changeSets[0].labels)
		assertNull(changeLog.changeSets[0].dbmsSet)
		assertTrue(changeLog.changeSets[0].isRunInTransaction)
		assertNull(changeLog.changeSets[0].failOnError)
		assertEquals("HALT", changeLog.changeSets[0].onValidationFail.toString())
	}

	/**
	 * Test creating a changeSet with all supported attributes.
	 */
	@Test
	fun changeSetFull() {
		val changeLog = buildChangeLog {
			changeSet(id = "monkey-change",
					  author = "stevesaliman",
					  dbms = "mysql",
					  runAlways = true,
					  runOnChange = true,
					  context = "testing",
					  labels = "test_label",
					  runInTransaction = false,
					  failOnError = true,
					  onValidationFail = ChangeSet.ValidationFailOption.MARK_RAN,
					  objectQuotingStrategy = ObjectQuotingStrategy.QUOTE_ONLY_RESERVED_WORDS) {
			  dropTable(tableName = "monkey")
			}
		}

		assertNotNull(changeLog.changeSets)
		assertEquals(1, changeLog.changeSets.size)
		assertEquals("monkey-change", changeLog.changeSets[0].id)
		assertEquals("stevesaliman", changeLog.changeSets[0].author)
		assertTrue(changeLog.changeSets[0].isAlwaysRun) // the property doesn"t match xml or docs.
		assertTrue(changeLog.changeSets[0].isRunOnChange)
		assertEquals(FILE_PATH, changeLog.changeSets[0].filePath)
		assertEquals("testing", changeLog.changeSets[0].contexts.contexts.first())
		assertEquals("test_label", changeLog.changeSets[0].labels.toString())
		assertEquals("mysql", changeLog.changeSets[0].dbmsSet.first())
		assertFalse(changeLog.changeSets[0].isRunInTransaction)
		assertTrue(changeLog.changeSets[0].failOnError)
		assertEquals("MARK_RAN", changeLog.changeSets[0].onValidationFail.toString())
		assertEquals(ObjectQuotingStrategy.QUOTE_ONLY_RESERVED_WORDS, changeLog.changeSets[0].objectQuotingStrategy)
	}

	/**
	 * Test creating a changeSet with all supported attributes, and one of them
	 * has an expression to expand.
	 */
	@Test
	fun changeSetFullWithProperties() {
		val changeLog = buildChangeLog {
			property(name = "authName", value = "stevesaliman")
			changeSet(id = "monkey-change",
					author = "\${authName}",
					dbms = "mysql",
					runAlways = true,
					runOnChange = true,
					context = "testing",
					labels = "test_label",
					runInTransaction = false,
					failOnError = true,
					onValidationFail = ChangeSet.ValidationFailOption.MARK_RAN,
					objectQuotingStrategy = ObjectQuotingStrategy.QUOTE_ONLY_RESERVED_WORDS) {
				dropTable(tableName = "monkey")
			}
		}

		assertNotNull(changeLog.changeSets)
		assertEquals(1, changeLog.changeSets.size)
		assertEquals("monkey-change", changeLog.changeSets[0].id)
		assertEquals("stevesaliman", changeLog.changeSets[0].author)
		assertTrue(changeLog.changeSets[0].isAlwaysRun) // the property doesn"t match xml or docs.
		assertTrue(changeLog.changeSets[0].isRunOnChange)
		assertEquals(FILE_PATH, changeLog.changeSets[0].filePath)
		assertEquals("testing", changeLog.changeSets[0].contexts.contexts.first())
		assertEquals("test_label", changeLog.changeSets[0].labels.toString())
		assertEquals("mysql", changeLog.changeSets[0].dbmsSet.first())
		assertFalse(changeLog.changeSets[0].isRunInTransaction)
		assertTrue(changeLog.changeSets[0].failOnError)
		assertEquals("MARK_RAN", changeLog.changeSets[0].onValidationFail.toString())
		assertEquals(ObjectQuotingStrategy.QUOTE_ONLY_RESERVED_WORDS, changeLog.changeSets[0].objectQuotingStrategy)
	}

	/**
	 * Test change log preconditions.  This uses the same delegate as change set
	 * preconditions, so we don"t have to do much here, just make sure we can
	 * call the correct thing from a change log and have the change log altered.
	 */
	@Test
	fun preconditionsInChangeLog() {
		val changeLog = buildChangeLog {
			preConditions {
				dbms(type = "mysql")
			}
		}

		assertEquals(0, changeLog.changeSets.size)
		assertNotNull(changeLog.preconditions)
		assertTrue(changeLog.preconditions.nestedPreconditions.all { precondition -> precondition is Precondition })
		assertEquals(1, changeLog.preconditions.nestedPreconditions.size)
		assertTrue(changeLog.preconditions.nestedPreconditions[0] is DBMSPrecondition)
		assertEquals("mysql", (changeLog.preconditions.nestedPreconditions[0] as DBMSPrecondition).type)
	}

	/**
	 * Try including a file that references an invalid changelog property
	 * in the the name.  In this case, the fileName property is not set, so it
	 * can"t be expanded and the parser will look for a file named
	 * "${fileName}.kotlin", which of course doesn"t exist.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun includeWithInvalidProperty() {
		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  include(file = """" + "\\\${fileName}" + """.kts")
  changeSet(author = "ssaliman", id = "ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")
		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)
	}

	/**
	 * Try including a file.
	 */
	@Test
	fun includeValid() {
		val includedChangeLogFile = createFileFrom(TMP_INCLUDE_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    runningAs(username = "ssaliman")
  }

  changeSet(author = "ssaliman", id = "included-change-set") {
    renameTable(oldTableName = "prosaic_table_name", newTableName = "monkey")
  }
}
""")

		var includedChangeLogFilePath = includedChangeLogFile.canonicalPath
		includedChangeLogFilePath = includedChangeLogFilePath.replace("\\", "/")

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  include(file = "$includedChangeLogFilePath")
  changeSet(author = "ssaliman", id = "ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(2, changeSets.size)
		assertEquals("included-change-set", changeSets[0].id)
		assertEquals("ROOT_CHANGE_SET", changeSets[1].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including a file that has a database changelog property in the name.
	 * This proves that we can expand tokens in filenames.
	 */
	@Test
	fun includeWithValidProperty() {
		val includedChangeLogFile = createFileFrom(TMP_INCLUDE_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    runningAs(username = "ssaliman")
  }

  changeSet(author = "ssaliman", id = "included-change-set") {
    renameTable(oldTableName = "prosaic_table_name", newTableName = "monkey")
  }
}
""")

		var includedChangeLogFilePath = includedChangeLogFile.canonicalPath
		includedChangeLogFilePath = includedChangeLogFilePath.replace("\\", "/")
		// Let"s strip off the extension so the include"s file includes a
		// property but is not just a property.
		val len = includedChangeLogFilePath.length
		val baseName = includedChangeLogFilePath.substring(0, len - 4)

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  property(name = "fileName", value = "$baseName")
  include(file = """" + "\\\${fileName}.kts" + """")
  changeSet(author = "ssaliman", id = "ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(2, changeSets.size)
		assertEquals("included-change-set", changeSets[0].id)
		assertEquals("ROOT_CHANGE_SET", changeSets[1].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including a file relative to the changelolg file.
	 */
	@Test
	fun includeRelative() {
		val includedChangeLogFile = createFileFrom(TMP_INCLUDE_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    runningAs(username = "ssaliman")
  }

  changeSet(author = "ssaliman", id = "included-change-set") {
    renameTable(oldTableName = "prosaic_table_name", newTableName = "monkey")
  }
}
""")

		val includedChangeLogFileName = includedChangeLogFile.name

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  include(file = "include/$includedChangeLogFileName", relativeToChangelogFile = true)
  changeSet(author = "ssaliman", id = "ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(2, changeSets.size)
		assertEquals("included-change-set", changeSets[0].id)
		assertEquals("ROOT_CHANGE_SET", changeSets[1].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all files in a directory.  For this test, we want a path
	 * that contains an invalid token.  The easiest way to do that is to
	 * simply use a token that doesn"t have a matching property.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun includeAllWithInvalidProperty() {
		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = """" + "\\\${includedChangeLogDir}" + """")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)
	}

	/**
	 * Try including all files in a directory.  For this test, we want 2 files
	 * to make sure we include them both, and in the right order.  Note: when
	 * other tests throw exceptions, this test may also fail because of unclean
	 * directories.  Fix the other tests first.
	 */
	@Test
	fun includeAllValid() {
		val includedChangeLogDir = createIncludedChangeLogFiles()

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = "$includedChangeLogDir")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(3, changeSets.size)
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(SECOND_INCLUDED_CHANGE_SET, changeSets[1].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[2].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all files in a directory.  For this test, we want 2 files
	 * to make sure we include them both, and in the right order.  Note: when
	 * other tests throw exceptions, this test may also fail because of unclean
	 * directories.  Fix the other tests first.
	 */
	@Test
	fun includeAllWithValidToken() {
		val includedChangeLogDir = createIncludedChangeLogFiles()

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  property(name = "includeDir", value = "$includedChangeLogDir")
  includeAll(path = """" + "\\\${includeDir}" + """")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(3, changeSets.size)
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(SECOND_INCLUDED_CHANGE_SET, changeSets[1].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[2].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all files in a directory, but with a resourceFilter.
	 * For this test, we"ll repeat want 2 files, but with a filter that
	 * excludes one of them. Test may fail because of unclean directories.
	 * Fix the other tests first.
	 */
	@Test
	fun includeAllValidWithFilter() {
		val includedChangeLogDir = createIncludedChangeLogFiles()

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = "$includedChangeLogDir",
             resourceFilter = "org.liquibase.kotlin.helper.IncludeAllFirstOnlyFilter")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")
		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(2, changeSets.size)  // from the first file, and the changelog itself.
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[1].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all files in a directory relative to the changelog.
	 */
	@Test
	fun includeAllRelative() {
		createIncludedChangeLogFiles()
		// For relative tests, the resource accessor needs to point to the
		// correct changelog directory.
		resourceAccessor = FileSystemResourceAccessor(TMP_CHANGELOG_DIR.absolutePath)
		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = "include", relativeToChangelogFile = true)
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(3, changeSets.size)
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(SECOND_INCLUDED_CHANGE_SET, changeSets[1].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[2].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all when the path doesn"t exist is invalid.  Expect an error.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun includeAllInvalidPath() {
		buildChangeLog {
			includeAll(path = "invalid")
		}
	}

	/**
	 * Try including all when the path doesn"t exist is invalid, but we"ve
	 * set the errorIfMissingOrEmpty property to false.  For this test, we"ll
	 * use a string to represent falseness.
	 */
	@Test
	fun includeAllInvalidPathIgnoreError() {
		val changeLog = buildChangeLog {
			includeAll(path = "invalid", errorIfMissingOrEmpty = false)
		}
		assertNotNull(changeLog)
		val changeSets = changeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(0, changeSets.size)
	}

	/**
	 * Try including all when the path is valid, but there are no usable files
	 * in the directory.  We"ll test this by using the filter to eliminate the
	 * one change set we"ll create to make sure we do the test after the filter.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun includeAllEmptyPath() {
		// This file should be excluded by the resource filter.
		val includedChangeLogFile = createFileFrom(TMP_INCLUDE_DIR, "second", "-2.kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  changeSet(author = "ssaliman", id = "$SECOND_INCLUDED_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(30)")
    }
  }
}
""")

		var includedChangeLogFilePath = includedChangeLogFile.parentFile.canonicalPath
		includedChangeLogFilePath = includedChangeLogFilePath.replace("\\", "/")

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = "$includedChangeLogFilePath",
             resourceFilter = "org.liquibase.kotlin.helper.IncludeAllFirstOnlyFilter")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)
	}

	/**
	 * Try including all when the path is valid, but there are no usable files
	 * in the directory.  This time, we"ll set the errorIfMissingOrEmpty
	 * property to false.  For this test, we"ll use a boolean to represent
	 * falseness.  We should get ignore the error about the empty directory,
	 * and get the root change set from the parent file.
	 */
	@Test
	fun includeAllEmptyPathIgnoreError() {
		// This file should be excluded by the resource filter.
		val includedChangeLogFile = createFileFrom(TMP_INCLUDE_DIR, "second", "-2.kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  changeSet(author = "ssaliman", id = "$SECOND_INCLUDED_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(30)")
    }
  }
}
""")

		var includedChangeLogFilePath = includedChangeLogFile.parentFile.canonicalPath
		includedChangeLogFilePath = includedChangeLogFilePath.replace("\\", "/")

		val rootChangeLogFile = createFileFrom(TMP_CHANGELOG_DIR, ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    dbms(type = "mysql")
  }
  includeAll(path = "$includedChangeLogFilePath", errorIfMissingOrEmpty = false,
             resourceFilter = "org.liquibase.kotlin.helper.IncludeAllFirstOnlyFilter")
  changeSet(author = "ssaliman", id = "$ROOT_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(50)")
    }
  }
}
""")

		val parser = parserFactory.getParser(rootChangeLogFile.absolutePath, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile.absolutePath, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(1, changeSets.size)  // from the changelog itself.
		assertEquals(ROOT_CHANGE_SET, changeSets[0].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
	}

	//----------------------------------------------------------------------
	// Tests of the includeAll method when the changelog file is accessed
	// via the classpath.


	/**
	 * Try including all files in a classpath directory.  We"ll want to make
	 * sure we include them both, and in the right order.
	 * <p>
	 * The change logs can"t be created on the fly, it must exist in a directory
	 * that is on the classpath, and we need to replace the resource accessor
	 * with one that can load a file from the classpath.
	 */
	@Test
	fun includeAllValidClasspath() {
		val rootChangeLogFile = "changelog.kts"
		resourceAccessor = ClassLoaderResourceAccessor()

		val parser = parserFactory.getParser(rootChangeLogFile, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(3, changeSets.size)
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(SECOND_INCLUDED_CHANGE_SET, changeSets[1].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[2].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all files in a classpath directory, but with a
	 * resourceFilter. For this test, we"ll have 2 files in the directory, but
	 * the resource filter will excludes one of them.
	 * <p>
	 * The change logs can"t be created on the fly, it must exist in a directory
	 * that is on the classpath, and we need to replace the resource accessor
	 * with one that can load a file from the classpath.
	 */
	@Test
	fun includeAllValidClasspathWithFilter() {
		val rootChangeLogFile = "filtered-changelog.kts"
		resourceAccessor = ClassLoaderResourceAccessor()

		val parser = parserFactory.getParser(rootChangeLogFile, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(2, changeSets.size)  // from the first file, and the changelog itself.
		assertEquals(FIRST_INCLUDED_CHANGE_SET, changeSets[0].id)
		assertEquals(ROOT_CHANGE_SET, changeSets[1].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(2, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
		assertTrue(preconditions[1] is RunningAsPrecondition)
	}

	/**
	 * Try including all from a classpath loaded change log when the include
	 * path doesn"t exist is invalid.  Expect an error.
	 * <p>
	 * The change logs can"t be created on the fly, it must exist in a directory
	 * that is on the classpath, and we need to replace the resource accessor
	 * with one that can load a file from the classpath.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun includeAllInvalidClassPath() {
		val rootChangeLogFile = "invalid-changelog.kts"
		resourceAccessor = ClassLoaderResourceAccessor()

		val parser = parserFactory.getParser(rootChangeLogFile, resourceAccessor)
		parser.parse(rootChangeLogFile, ChangeLogParameters(), resourceAccessor)
	}

	/**
	 * Try including all from a classpath loaded change log when the include
	 * path is invalid, but we"ve set the errorIfMissingOrEmpty property to
	 * false.
	 * <p>
	 * The change logs can"t be created on the fly, it must exist in a directory
	 * that is on the classpath, and we need to replace the resource accessor
	 * with one that can load a file from the classpath.
	 */
	@Test
	fun includeAllInvalidClassPathIgnoreError() {
		val rootChangeLogFile = "ignore-changelog.kts"
		resourceAccessor = ClassLoaderResourceAccessor()

		val parser = parserFactory.getParser(rootChangeLogFile, resourceAccessor)
		val rootChangeLog = parser.parse(rootChangeLogFile, ChangeLogParameters(), resourceAccessor)

		assertNotNull(rootChangeLog)
		val changeSets = rootChangeLog.changeSets
		assertNotNull(changeSets)
		assertEquals(1, changeSets.size)  // from the first file, and the changelog itself.
		assertEquals(ROOT_CHANGE_SET, changeSets[0].id)

		val preconditions = rootChangeLog.preconditions.nestedPreconditions
		assertNotNull(preconditions)
		assertEquals(1, preconditions.size)
		assertTrue(preconditions[0] is DBMSPrecondition)
	}

	/**
	 * Try including a property from a file that doesn"t exist.
	 */
	@Test(expected = ChangeLogParseException::class)
	fun propertyFromInvalidFile() {
		buildChangeLog {
			property(file = "$TMP_CHANGELOG_DIR/bad.properties")
		}
	}

	/**
	 * Try including a property from a file when we don"t hae a dbms or context.
	 */
	@Test
	fun propertyFromFilePartial() {
		val propertyFile = createFileFrom(TMP_CHANGELOG_DIR, ".properties", """
emotion=angry
""")
		val propertyFilePath = propertyFile.canonicalPath.replace("\\", "/")

		val changeLog = buildChangeLog {
			property(file = propertyFilePath)
		}

		// change log parameters are not exposed through the API, so get them
		// using reflection.  Also, there are
		val value = changeLog.changeLogParameters!!.getValue("emotion", changeLog)
		assertEquals("angry", value)
	}

	/**
	 * Helper method that builds a changeSet from the given closure.  Tests will
	 * use this to test parsing the various closures that make up the Kotlin DSL.
	 * @param closure the closure containing changes to parse.
	 * @return the changeSet, with parsed changes from the closure added.
	 */
	private fun buildChangeLog(closure: (KotlinDatabaseChangeLog).() -> Unit): DatabaseChangeLog {
		val changelog = DatabaseChangeLog(FILE_PATH)
		changelog.changeLogParameters = ChangeLogParameters()
		val ktChangeLog = KotlinDatabaseChangeLog(changelog)
		ktChangeLog.resourceAccessor = FileSystemResourceAccessor()
		ktChangeLog.closure()
		return changelog
	}

	/**
	 * Helper method to create changelogs in a directory for testing the
	 * includeAll methods.  It creates 3 files:
	 * <ul>
	 * <li>2 kotlin files that should be included with an includeAll</li>
	 * <li>An xml file that should be excluded from the includeAll</li>
	 * </ul>
	 * @return the full path of the directory where the files were placed.
	 */
	private fun createIncludedChangeLogFiles(): String {
		createFileFrom(TMP_INCLUDE_DIR, "first", ".kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  preConditions {
    runningAs(username = "ssaliman")
  }

  changeSet(author = "ssaliman", id = "$FIRST_INCLUDED_CHANGE_SET") {
    renameTable(oldTableName = "prosaic_table_name", newTableName = "monkey")
  }
}
""")

		createFileFrom(TMP_INCLUDE_DIR, "second", "-2.kts", """
import org.liquibase.kotlin.*
databaseChangeLog {
  changeSet(author = "ssaliman", id = "$SECOND_INCLUDED_CHANGE_SET") {
    addColumn(tableName = "monkey") {
      column(name = "emotion", type = "varchar(30)")
    }
  }
}
""")

		createFileFrom(TMP_INCLUDE_DIR, "third", "-3.xml", """
<databaseChangeLog>
  <changeSet author="ssaliman" id="included-change-set-3">
    <addColumn tableName="monkey">
      <column name="gender" type="varchar(1)"/>
    </addColumn>
  </changeSet>
</databaseChangeLog>
""")

		return TMP_INCLUDE_DIR.canonicalPath.replace("\\", "/")
	}

	private fun createFileFrom(directory: File, suffix: String, text: String): File {
		return createFileFrom(directory, "liquibase-", suffix, text)
	}

	private fun createFileFrom(directory: File, prefix: String, suffix: String, text: String): File {
		val file = File.createTempFile(prefix, suffix, directory)
		file.writer().use {
			it.append(text)
		}
		return file
	}
}

