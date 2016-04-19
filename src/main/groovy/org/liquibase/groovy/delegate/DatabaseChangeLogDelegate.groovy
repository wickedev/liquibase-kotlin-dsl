/*
 * Copyright 2011-2016 Tim Berglund and Steven C. Saliman
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

import liquibase.parser.ParsedNode

import java.text.ParseException

/**
 * This class is the delegate for the {@code databaseChangeLog} element.  It
 * is the starting point for parsing the Groovy DSL.
 *
 * @author Steven C. Saliman
 */
class DatabaseChangeLogDelegate {
	def parentNode = null


	DatabaseChangeLogDelegate(parentNode) {
		this.parentNode = parentNode
	}

	def propertyMissing(String name) {
		println "Missing property $name"
	}

	/**
	 * Groovy calls methodMissing when it can't find a matching method to call.
	 * We use it to tell the user which changeSet had the invalid element.
	 * @param name the name of the method Groovy wanted to call.
	 * @param args the original arguments to that method.
	 */
	def methodMissing(String name, args) {
		// start by adding a node for the method itself to the current node
		def node
		if ( parentNode == null ) {
			// The first method that runs defines the root node.
			parentNode = ParsedNode.createRootNode(name)
			node = parentNode
		} else {
			node = parentNode.addChild(name)
		}

		if ( args != null ) {
			args.each { arg ->
				if ( arg instanceof Map ) {
					arg.each { key, value ->
						node.addChild(key).setValue(value)
					}
				} else if ( arg instanceof Closure ) {
					arg.resolveStrategy = Closure.DELEGATE_FIRST
					def delegate = new DatabaseChangeLogDelegate(node)
					arg.delegate = delegate
					arg.call()
				} else {
					node.value = arg
				}
			}
		}
	}

	// sqlFile: disallow the sql attribute

	// addForeignKeyConstraint: referencesUniqueColumn is deprecated - stop checking

	// changeSet: alwaysRun is invalid - maybe stop checking

	// include: disallow path attr: see what LB 4 does with it

	//createStoredProcedure: disallow, or stop checking

	// loadData: fail if any attrs are file objects

	// loadUpdateData: fail if any attrs are file objects

	// arg: has non-map ver that sets "value" child node to String val

	// sql (map, closure): create sql node with map children and value of closure as value

	// sql (closure): create sql node with closure value as value

	// sql { comment("commentText") sqlText }: create sql node with value and comment child.

	// stop 'msgText': call stop(message: msgText)

	// Any method in CustomPrecondition -> ParamNode w name and value children

	//

}
