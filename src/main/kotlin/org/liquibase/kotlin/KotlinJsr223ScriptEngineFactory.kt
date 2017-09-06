package org.liquibase.kotlin

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.utils.PathUtil
import org.liquibase.kotlin.internal.script.*
import java.io.File
import java.io.FileNotFoundException
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.templates.standard.ScriptTemplateWithArgs

private const val KOTLIN_COMPILER_JAR = "kotlin-compiler.jar"
private const val KOTLIN_JAVA_STDLIB_JAR = "kotlin-stdlib.jar"
private const val KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar"

class KotlinJsr223ScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
	override fun getScriptEngine(): ScriptEngine =
			KotlinJsr223JvmLocalScriptEngine(
					Disposer.newDisposable(),
					this,
					scriptCompilationClasspathFromContext("kotlin-compiler-embeddable.jar"),
					KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
					{ ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
					arrayOf(Bindings::class)
			)
}

private val validJarExtensions = setOf("jar", "zip")

private fun scriptCompilationClasspathFromContext(keyName: String, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<File> =
		(System.getProperty("kotlin.script.classpath")?.split(File.pathSeparator)?.map(::File)
				?: contextClasspath(keyName, classLoader)
				).let {
			it?.plus(kotlinScriptStandardJars) ?: kotlinScriptStandardJars
		}
				.mapNotNull { it?.canonicalFile }
				.distinct()
				.filter { (it.isDirectory || (it.isFile && it.extension.toLowerCase() in validJarExtensions)) && it.exists() }

private val kotlinCompilerJar: File by lazy {
	// highest prio - explicit property
	System.getProperty("kotlin.compiler.jar")?.let(::File)?.existsOrNull()
			// search classpath from context classloader and `java.class.path` property
			?: (classpathFromClass(Thread.currentThread().contextClassLoader, K2JVMCompiler::class)
			?: contextClasspath(KOTLIN_COMPILER_JAR, Thread.currentThread().contextClassLoader)
			?: classpathFromClasspathProperty()
			)?.firstOrNull { it.matchMaybeVersionedFile(KOTLIN_COMPILER_JAR) }
			?: throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.jar property to proper location")
}

private val kotlinStdlibJar: File? by lazy {
	System.getProperty("kotlin.java.runtime.jar")?.let(::File)?.existsOrNull()
			?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_STDLIB_JAR) }.existsOrNull()
			?: PathUtil.getResourcePathForClass(JvmStatic::class.java).existsOrNull()
}

private val kotlinScriptRuntimeJar: File? by lazy {
	System.getProperty("kotlin.script.runtime.jar")?.let(::File)?.existsOrNull()
			?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR) }.existsOrNull()
			?: PathUtil.getResourcePathForClass(ScriptTemplateWithArgs::class.java).existsOrNull()
}

private val kotlinScriptStandardJars by lazy { listOf(kotlinStdlibJar, kotlinScriptRuntimeJar) }