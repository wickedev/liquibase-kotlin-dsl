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

package liquibase.parser.ext

import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import liquibase.parser.ChangeLogParser
import liquibase.resource.ResourceAccessor
import org.liquibase.kotlin.KotlinDatabaseChangeLogDefinition

/**
 * This is the main parser class for the Liquibase Kotlin DSL.  It is the
 * integration point to Liquibase itself.  It must be in the
 * liquibase.parser.ext package to be found by Liquibase at runtime.
 *
 * @author Tim Berglund
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
@Suppress("unused")
open class KotlinLiquibaseChangeLogParser : ChangeLogParser {
    override fun parse(physicalChangeLogLocation: String, changeLogParameters: ChangeLogParameters?, resourceAccessor: ResourceAccessor): DatabaseChangeLog {
        val clazz = try {
            KotlinLiquibaseChangeLogParser::class.java.classLoader.loadClass(physicalChangeLogLocation.removeSuffix(".kt"))
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("$physicalChangeLogLocation is not a class", e)
        }
        if (clazz.isAssignableFrom(KotlinDatabaseChangeLogDefinition::class.java)) {
            throw RuntimeException("$physicalChangeLogLocation is not a class implementing ${KotlinDatabaseChangeLogDefinition::class.java.simpleName}")
        }
        if (clazz.getConstructor() == null) {
            throw RuntimeException("$physicalChangeLogLocation needs to have a public no-arg constructor")
        }
        return (clazz.newInstance() as KotlinDatabaseChangeLogDefinition).define()
    }

    override fun supports(changeLogFile: String, resourceAccessor: ResourceAccessor): Boolean {
        return changeLogFile.endsWith(".kt")
    }

    override fun getPriority(): Int = ChangeLogParser.PRIORITY_DEFAULT
}