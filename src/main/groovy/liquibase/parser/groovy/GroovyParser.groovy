/*
 * Copyright 2011-2015 Tim Berglund and Steven C. Saliman
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

package liquibase.parser.groovy

import liquibase.Scope
import liquibase.parser.AbstractParser
import liquibase.parser.ParsedNode
import liquibase.exception.ParseException

import liquibase.parser.groovy.delegate.DatabaseChangeLogDelegate
import org.codehaus.groovy.control.CompilerConfiguration

/**
 * This is the main parser class for the Liquibase Groovy DSL.  It is the
 * integration point to Liquibase itself.  It must be in the
 * liquibase.parser.groovy package to be found by Liquibase at runtime.
 *
 * @author Tim Berglund
 * @author Steven C. Saliman
 */
class GroovyParser extends AbstractParser {


	@Override
	public int getPriority(String path, Scope scope) {
		if ( path.toLowerCase().endsWith(".groovy") && scope.getResourceAccessor() != null ) {
			return PRIORITY_DEFAULT;
		}
		return PRIORITY_NOT_APPLICABLE;
	}

	@Override
	ParsedNode parse(String physicalChangeLogLocation,
	                 Scope scope) {

		physicalChangeLogLocation = physicalChangeLogLocation.replaceAll('\\\\', '/')

		def inputStreams
		try {
			inputStreams = scope.resourceAccessor.openStreams(physicalChangeLogLocation as String)

		} catch (Exception e) {
			throw new ParseException(e);
		}

		if ( inputStreams == null || inputStreams.size() == 0 ) {
			throw new ParseException("${physicalChangeLogLocation} does not exist")
		} else if ( inputStreams.size() > 1 ) {
			throw new ParseException("Found ${inputStreams.size()} files that match ${path}");
		}

		def inputStream = inputStreams.toArray()[0]

		try {
//			def binding = new Binding()
//			def shell = new GroovyShell(binding)
			def cc = new CompilerConfiguration()
			cc.setScriptBaseClass(DelegatingScript.class.getName())
			GroovyShell shell = new GroovyShell(new Binding(), cc)

			// Parse the script, give it the local changeLog instance, give it access
			// to root-level method delegates, and call.

			DelegatingScript script = (DelegatingScript) shell.parse(new InputStreamReader(inputStream, "UTF8"))
			DatabaseChangeLogDelegate delegate = new DatabaseChangeLogDelegate(physicalChangeLogLocation, null)
			script.setDelegate(delegate);
			script.run();
			def rootNode = delegate.parentNode
			rootNode.addChild("physicalPath").setValue(physicalChangeLogLocation)

			// The rootNode will have been populated by the script
			return rootNode
		}
		finally {
			try {
				inputStream.close()
			}
			catch (Exception e) {
				// Can't do much more than hope for the best here
			}
		}
	}

	@Override
	String describeOriginal(ParsedNode parsedNode) {
		// TODO: Walk up the parsed node to get the change set name.
		return null
	}

	def getChangeLogMethodMissing() {
		{ name, args ->
			if ( name == 'databaseChangeLog' ) {
				processDatabaseChangeLogRootElement(databaseChangeLog, resourceAccessor, args)
			} else {
				throw new ParseException("Unrecognized root element ${name}")
			}
		}
	}

	private
	def processDatabaseChangeLogRootElement(databaseChangeLog, resourceAccessor, args) {
		def delegate;
		def closure;

		switch ( args.size() ) {
			case 0:
				throw new ParseException("databaseChangeLog element cannot be empty")

			case 1:
				closure = args[0]
				if ( !(closure instanceof Closure) ) {
					throw new ParseException("databaseChangeLog element must be followed by a closure (databaseChangeLog { ... })")
				}
				delegate = new DatabaseChangeLogDelegate(databaseChangeLog)
				break

			case 2:
				def params = args[0]
				closure = args[1]
				if ( !(params instanceof Map) ) {
					throw new ParseException("databaseChangeLog element must take parameters followed by a closure (databaseChangeLog(key: value) { ... })")
				}
				if ( !(closure instanceof Closure) ) {
					throw new ParseException("databaseChangeLog element must take parameters followed by a closure (databaseChangeLog(key: value) { ... })")
				}
				delegate = new DatabaseChangeLogDelegate(params, databaseChangeLog)
				break

			default:
				throw new ParseException("databaseChangeLog element has too many parameters: ${args}")
		}

		delegate.resourceAccessor = resourceAccessor
		closure.delegate = delegate
		closure.resolveStrategy = Closure.OWNER_FIRST
		closure.call()
	}
}

