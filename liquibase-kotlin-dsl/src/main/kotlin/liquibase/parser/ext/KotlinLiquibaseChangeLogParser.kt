package liquibase.parser.ext

import com.faendir.liquibase.ChangelogScript
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import liquibase.parser.ChangeLogParser
import liquibase.resource.ResourceAccessor
import org.liquibase.kotlin.KotlinDatabaseChangeLog

@Suppress("unused")
open class KotlinLiquibaseChangeLogParser : ChangeLogParser {
    override fun parse(physicalChangeLogLocation: String, changeLogParameters: ChangeLogParameters?, resourceAccessor: ResourceAccessor): DatabaseChangeLog {
        val clazz = try {
            KotlinLiquibaseChangeLogParser::class.java.classLoader.loadClass(scriptToClassName(physicalChangeLogLocation))
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("$physicalChangeLogLocation is not a class", e)
        }
        if (clazz.isAssignableFrom(ChangelogScript::class.java)) {
            throw RuntimeException("$physicalChangeLogLocation is not a class implementing ${ChangelogScript::class.java.simpleName}")
        }
        val instance = clazz.constructors.single().newInstance()
        val resultField = clazz.getDeclaredField("\$\$result").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val pair = resultField.get(instance) as? Pair<String?, ((KotlinDatabaseChangeLog).() -> Unit)?> ?: throw IllegalArgumentException("script didn't return changelog")
        val changeLog = DatabaseChangeLog(pair.first ?: physicalChangeLogLocation)
        changeLog.changeLogParameters = changeLogParameters
        val ktChangeLog = KotlinDatabaseChangeLog(changeLog)
        ktChangeLog.resourceAccessor = resourceAccessor
        pair.second?.let { ktChangeLog.it() }
        return changeLog
    }

    override fun supports(changeLogFile: String, resourceAccessor: ResourceAccessor): Boolean {
        return changeLogFile.endsWith(".changelog.kts")
    }

    override fun getPriority(): Int = ChangeLogParser.PRIORITY_DEFAULT

    private fun scriptToClassName(scriptName: String) : String = scriptName.removeSuffix(".kts").let {
        "${it.substringBeforeLast('/').replace('/', '.')}.${it.substringAfterLast('/').replace('.', '_').capitalize()}"
    }
}