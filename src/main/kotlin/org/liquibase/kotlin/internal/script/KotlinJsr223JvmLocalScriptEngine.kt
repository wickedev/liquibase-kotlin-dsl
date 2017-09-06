package org.liquibase.kotlin.internal.script

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import kotlin.reflect.KClass

internal class KotlinJsr223JvmLocalScriptEngine(
		disposable: Disposable,
		factory: ScriptEngineFactory,
		private val templateClasspath: List<File>,
		templateClassName: String,
		private val getScriptArgs: (ScriptContext, Array<out KClass<out Any>>?) -> ScriptArgsWithTypes?,
		private val scriptArgsTypes: Array<out KClass<out Any>>?
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223JvmInvocableScriptEngine {

	override val replCompiler: ReplCompiler by lazy {
		GenericReplCompiler(
				disposable,
				makeScriptDefinition(templateClasspath, templateClassName),
				makeCompilerConfiguration(),
				PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
	}

	private val localEvaluator by lazy { GenericReplCompilingEvaluator(replCompiler, templateClasspath, Thread.currentThread().contextClassLoader, getScriptArgs(getContext(), scriptArgsTypes)) }

	override val replEvaluator: ReplFullEvaluator get() = localEvaluator

	override val state: IReplStageState<*> get() = getCurrentState(getContext())

	override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = replEvaluator.createState(lock)

	override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = getScriptArgs(context, scriptArgsTypes)

	private fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition {
		val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this.javaClass.classLoader)
		val cls = classloader.loadClass(templateClassName)
		return KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, null, null, emptyMap())
	}

	private fun makeCompilerConfiguration() = CompilerConfiguration().apply {
		addJvmClasspathRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
		addJvmClasspathRoots(templateClasspath)
		put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
		languageVersionSettings = LanguageVersionSettingsImpl(
				LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, mapOf(AnalysisFlag.skipMetadataVersionCheck to true)
		)
	}
}