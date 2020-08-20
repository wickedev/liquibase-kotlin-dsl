package com.faendir.liquibase

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

class ChangelogScriptConfiguration : ScriptCompilationConfiguration( {
  defaultImports("org.liquibase.kotlin.*")
  ide {
    acceptedLocations(ScriptAcceptedLocation.Everywhere)
  }
  jvm {
    dependenciesFromClassContext(ChangelogScript::class, wholeClasspath = true)
  }
})

@KotlinScript(fileExtension = "changelog.kts", compilationConfiguration = ChangelogScriptConfiguration::class)
abstract class ChangelogScript