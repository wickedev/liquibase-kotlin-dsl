package org.liquibase.kotlin

import liquibase.ContextExpression
import liquibase.Labels
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.IncludeAllFilter
import liquibase.database.ObjectQuotingStrategy
import liquibase.exception.ChangeLogParseException
import liquibase.parser.ChangeLogParserFactory
import liquibase.precondition.core.PreconditionContainer
import liquibase.resource.ResourceAccessor
import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import java.util.*

class KotlinDatabaseChangeLog(databaseChangeLog: DatabaseChangeLog) : BaseObject(databaseChangeLog), Serializable {
    internal var resourceAccessor: ResourceAccessor? = null

    fun preConditions(onFail: PreconditionContainer.FailOption? = null,
                      onError: PreconditionContainer.ErrorOption? = null,
                      onUpdateSQL: PreconditionContainer.OnSqlOutputOption? = null,
                      onFailMessage: String? = null,
                      onErrorMessage: String? = null,
                      closure: ((KotlinPrecondition).() -> Unit)? = null) {
        val preconditions = KotlinPrecondition(onFail, onError, onUpdateSQL, onFailMessage?.eval(), onErrorMessage?.eval(), databaseChangeLog)

        closure?.let {
            preconditions.it()
        }

        databaseChangeLog.preconditions = preconditions.preconditions
    }

    fun property(
            name: String? = null,
            value: String? = null,
            context: String? = null,
            labels: String? = null,
            dbms: String? = null,
            global: Boolean = true,
            file: String? = null) {

        val ctx: ContextExpression? = if (context != null) ContextExpression(context) else null
        val lbls: Labels? = if (labels != null) Labels(labels) else null

        if (file == null) {
            databaseChangeLog.changeLogParameters.set(name, value, ctx, lbls, dbms, global, databaseChangeLog)
        } else {
            val props = Properties()
            val propertiesAsStreams = resourceAccessor?.getResourcesAsStream(file)
                    ?: throw ChangeLogParseException("Unable to load file with properties: $file")

            propertiesAsStreams.forEach { stream ->
                props.load(stream)
                props.forEach { k, v ->
                    databaseChangeLog.changeLogParameters.set(k as String, v as String, ctx, lbls, dbms, global, databaseChangeLog)
                }
            }
        }
    }

    /**
     * Process the include element to include a file with change sets.
     */
    fun include(file: String, relativeToChangelogFile: Boolean = false) {
        var physicalChangeLogLocation = databaseChangeLog.physicalFilePath.replace(System.getProperty("user.dir").toString() + "/", "")

        var fileLocation = file
        if (relativeToChangelogFile && (physicalChangeLogLocation.contains("/") || physicalChangeLogLocation.contains("\\"))) {
            physicalChangeLogLocation = physicalChangeLogLocation.replace('\\', '/')
            fileLocation = physicalChangeLogLocation.replace(Regex("/[^/]*\$"), "") + "/" + fileLocation
        }

        val fileName = fileLocation.eval()

        includeChangeLog(fileName)
    }

    /**
     * Process the includeAll element to include all kotlin files in a directory.
     */
    fun includeAll(path: String,
                   relativeToChangelogFile: Boolean = false,
                   errorIfMissingOrEmpty: Boolean = true,
                   resourceFilter: String? = null) {

        var filter: IncludeAllFilter? = null
        if (resourceFilter != null) {
            val filterName = resourceFilter.eval()
            filter = Class.forName(filterName).newInstance() as IncludeAllFilter
        }

        val pathName = path.eval()

        loadIncludedChangeSets(pathName, relativeToChangelogFile, filter,
                errorIfMissingOrEmpty, getStandardChangeLogComparator())
    }

    fun changeSet(
            id: String,
            author: String,
            runAlways: Any = false,
            runOnChange: Any = false,
            context: String? = null,
            dbms: String? = null,
            labels: String? = null,
            runInTransaction: Any = true,
            failOnError: Any? = null,
            onValidationFail: ChangeSet.ValidationFailOption? = null,
            objectQuotingStrategy: ObjectQuotingStrategy? = null,
            closure: ((KotlinChangeSet).() -> Unit)? = null) {

        val cs = KotlinChangeSet(
                id.eval(),
                author.eval(),
                runAlways.evalBool(),
                runOnChange.evalBool(),
                context?.eval(),
                dbms?.eval(),
                labels?.eval(),
                runInTransaction.evalBool(),
                databaseChangeLog,
                failOnError?.evalBool(),
                onValidationFail,
                objectQuotingStrategy)

        cs.resourceAccessor = resourceAccessor
        closure?.let {
            cs.it()
        }
    }

    /**
     * Helper method to do the actual work of including a databaseChangeLog file.
     * @param filename the file to include.
     */
    private fun includeChangeLog(filename: String) {
        val parser = ChangeLogParserFactory.getInstance().getParser(filename, resourceAccessor)
        val includedChangeLog = parser.parse(filename, databaseChangeLog.changeLogParameters, resourceAccessor)
        includedChangeLog?.changeSets?.forEach { changeSet ->
            databaseChangeLog.addChangeSet(changeSet)
        }
        includedChangeLog?.preconditions?.nestedPreconditions?.forEach { precondition ->
            databaseChangeLog.preconditions.addNestedPrecondition(precondition)
        }
    }

    /**
     * Helper method to load all the kotlin changesets in a directory.
     * @param dir the name of the directory whose change sets we want.
     * @param isRelativeToChangelogFile whether or not the dirName is relative
     *        to the original change log.
     * @param resourceFilter a filter through which to run each file name.  This
     *        filter can decide whether or not a given file should be processed.
     * @param errorIfMissingOrEmpty whether or not we should stop parsing if
     *        the given directory is empty.
     * @param resourceComparator a comparator to use for sorting filenames.
     */
    private fun loadIncludedChangeSets(dir: String, isRelativeToChangelogFile: Boolean,
                                       resourceFilter: IncludeAllFilter?,
                                       errorIfMissingOrEmpty: Boolean,
                                       resourceComparator: Comparator<String>) {
        try {
            var dirName = dir.replace('\\', '/')

            if (!dirName.endsWith("/")) {
                dirName += '/'
            }

            var relativeTo: String? = null
            if (isRelativeToChangelogFile) {
                val parent = File(databaseChangeLog.physicalFilePath).parent
                if (parent != null) {
                    relativeTo = parent + '/' + dirName
                }
            }

            var unsortedResources: Set<String>? = null
            try {
                unsortedResources = resourceAccessor?.list(relativeTo, dirName, true, false, true)
            } catch (e: FileNotFoundException) {
                if (errorIfMissingOrEmpty) {
                    throw ChangeLogParseException("DatabaseChangeLog: includeAll path '$dirName does not exist, or has no files that match the filter.")
                }
            }

            val resources: MutableSet<String> = java.util.TreeSet(resourceComparator)
            if (unsortedResources != null) {
                unsortedResources.forEach { resource ->
                    if (resourceFilter == null || resourceFilter.include(resource)) {
                        resources.add(resource)
                    }
                }
            }

            if (resources.size == 0 && errorIfMissingOrEmpty) {
                throw ChangeLogParseException("DatabaseChangeLog: includeAll path '$dirName does not exist, or has no files that match the filter.")
            }

            // Filter the list to just the kotlin files and include each one.
            resources.filter { it.endsWith(".kts") }.forEach {
                includeChangeLog(it)
            }
        } catch (e: Exception) {
            throw ChangeLogParseException("DatabaseChangeLog: error processing includeAll path '$dir.", e)
        }
    }

    private fun getStandardChangeLogComparator(): Comparator<String> {
        return Comparator { o1, o2 ->
            o1.compareTo(o2)
        }
    }
}