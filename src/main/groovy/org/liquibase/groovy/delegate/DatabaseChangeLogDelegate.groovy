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
	def fileName = null
	def parentNode = null


	DatabaseChangeLogDelegate(fileName, parentNode) {
		this.fileName = fileName
		this.parentNode = parentNode
	}

	/**
	 * Groovy calls methodMissing when it can't find a matching method to call.
	 * We use it to create the appropriate type of ParseNode
	 * @param name the name of the method Groovy wanted to call.
	 * @param args the original arguments to that method.
	 * @return the node created by this method.  Some actions, like
	 * createProcedure, will require extra handling after basic processing.
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
		node.fileName = fileName

		if ( args != null ) {
			args.each { arg ->
				if ( arg instanceof Map ) {
					arg.each { key, value ->
						node.addChild(key).setValue(value)
					}
				} else if ( arg instanceof Closure ) {
					// Some actions use the closure as a value instead of a
					// new child node.
					if ( closureIsValue(name) ) {
						node.value = arg.call()
					} else {
						arg.resolveStrategy = Closure.DELEGATE_FIRST
						def delegate = new DatabaseChangeLogDelegate(fileName, node)
						arg.delegate = delegate
						arg.call()
					}
				} else {
					node.value = arg
				}
			}
		}
		return node
	}

	/**
	 * A {@code customChange} action doesn't fit the standard rules.  It's
	 * closure has a series of parameters that need to be passed into the
	 * custom change class, which need to be processed with a different
	 * delegate.
	 * @param params the parameter map with the class name.
	 * @param closure the closure containing the parameters to the custom
	 * class.
	 * @return the ParsedNode created by this action.
	 */
	def customChange(Map attributes, Closure closure = null) {
		def node = parentNode.addChild('customChange')
		attributes.each { key, value ->
			node.addChild(key).setValue(value)
		}
		if ( closure ) {
			def delegate = new KeyValueDelegate()
			closure.delegate = delegate
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure.call()
			delegate.map.each { key, value ->
				def paramNode = node.addChild('param')
				paramNode.addChild('name').setValue(key)
				paramNode.addChild('value').setValue(value)
			}
		}
		return node
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

	/**
	 * In most cases, closures of an action are used to define child nodes,
	 * but in some cases, like createProcedure or sql, the closure is used as
	 * the node value instead of a child node.  This helper method decides which
	 * is which.
	 * @param action the action in question.
	 * @return {@code true} if the given action's closure should be put in the
	 * value of the ParsedNode instead of a child node.
	 */
	private boolean closureIsValue(action) {
		return ( action == 'createProcedure'
				|| action == 'createView'
				|| action == 'sql')

	}

}
