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

import liquibase.ContextExpression
import liquibase.Labels
import liquibase.changelog.DatabaseChangeLog
import liquibase.exception.ChangeLogParseException
import liquibase.sql.visitor.*

/**
 * This delegate handles the Liquibase ModifySql element, which can be used
 * to tweak the SQL that Liquibase generates.
 *
 * @author Steven C. Saliman
 * @author Jason Blackwell
 */
class KotlinModifySql(dbms: String? = null, context: String? = null, labels: String? = null, applyToRollback: Any? = null,
						databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog) {
	private val modifySqlDbmsList: Set<String>? = dbms?.eval()?.replace(" ", "")?.split(',')?.toSet()
	private val modifySqlAppliedOnRollback: Boolean? = applyToRollback?.evalBool()
	private val modifySqlContexts = context?.eval()?.let { ContextExpression(it) }
	private val modifySqlLabels = labels?.eval()?.let { Labels(it) }

	internal val sqlVisitors: MutableList<SqlVisitor> = ArrayList()

	fun prepend(value: String) {
		createSqlVisitor<PrependSqlVisitor>("prepend") {
			it.value = value.eval()
		}
	}

	fun append(value: String) {
		createSqlVisitor<AppendSqlVisitor>("append") {
			it.value = value.eval()
		}
	}

	fun replace(replace: String, with: String) {
		createSqlVisitor<ReplaceSqlVisitor>("replace") {
			it.replace = replace.eval()
			it.with = with.eval()
		}
	}

	fun regExpReplace(replace: String, with: String) {
		createSqlVisitor<RegExpReplaceSqlVisitor>("regExpReplace") {
			it.replace = replace.eval()
			it.with = with.eval()
		}
	}

	private fun <T : SqlVisitor> createSqlVisitor(type: String, propertySetter: (T) -> Unit) {
		val sqlVisitor = SqlVisitorFactory.getInstance().create(type) as? T ?: throw ChangeLogParseException("sql visitor of type $type failed to cast")

		propertySetter(sqlVisitor)

		if (modifySqlDbmsList != null) {
			sqlVisitor.applicableDbms = modifySqlDbmsList
		}
		if (modifySqlContexts != null) {
			sqlVisitor.contexts = modifySqlContexts
		}
		if (modifySqlAppliedOnRollback != null) {
			sqlVisitor.isApplyToRollback = modifySqlAppliedOnRollback
		}
		if (modifySqlLabels != null) {
			sqlVisitor.labels = modifySqlLabels
		}

		sqlVisitors.add(sqlVisitor)
	}
}

