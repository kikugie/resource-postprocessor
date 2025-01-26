@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalContracts::class)

package dev.kikugie.postprocess.gradle

import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object PostProcessorGroup : ProblemGroup by BasicProblemGroup("postprocessor", "Resource Postprocessor Reports")

data class BasicProblemGroup(
    @JvmField val name: String,
    @JvmField val display: String,
    @JvmField val parent: ProblemGroup? = null
) : ProblemGroup {
    override fun getName(): String = name
    override fun getDisplayName(): String = display
    override fun getParent(): ProblemGroup? = parent
}

class ProblemID {
    lateinit var name: String
    lateinit var display: String
    var parent: ProblemGroup? = null

    fun asGroup(): ProblemGroup = BasicProblemGroup(name, display, parent)
}

fun ProblemGroup.applyToSpec(spec: ProblemSpec) =
    if (parent == null) spec.id(name, displayName)
    else spec.id(name, displayName, parent!!)

fun Problems.verify(state: Boolean, spec: ProblemSpec.() -> Unit) {
    contract { returns() implies state }
    if (!state) throw reporter.throwing(spec)
}

fun <T> Problems.verifyNotNull(value: T?, spec: ProblemSpec.() -> Unit): T {
    contract { returns() implies (value != null) }
    return value ?: throw reporter.throwing(spec)
}

fun <T> Result<T>.getOrReport(problems: Problems, spec: ProblemSpec.(Throwable) -> Unit) = getOrElse {
    throw problems.reporter.throwing { spec(it) }
}

inline fun ProblemGroup.child(builder: ProblemID.() -> Unit) =
    ProblemID().apply(builder).apply { parent = this@child }.asGroup()