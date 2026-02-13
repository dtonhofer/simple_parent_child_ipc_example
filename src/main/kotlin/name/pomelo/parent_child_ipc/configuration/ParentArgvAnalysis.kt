package name.pomelo.parent_child_ipc.configuration

import name.pomelo.parent_child_ipc.ParentMain
import name.pomelo.parent_child_ipc.ParentMain.parentMarker
import name.pomelo.parent_child_ipc.helpers.Logging

object ParentArgvAnalysis {

    private const val configOption = "--config"
    private const val withParentAccidentsOption = "--with-parent-accidents" // specific to parent
    private const val withChildAccidentsOption = "--with-child-accidents" // specific to parent

    private enum class ArgvState { NO_EXPECTATION, EXPECT_CONFIG_FILE, BEYOND_DASH_DASH }

    data class ArgvAnalysisResult(
        val configFilePath: String,
        val withParentAccidents: Boolean,
        val withChildAccidents: Boolean,
        val discardedArgv: List<String>,
        val argsBeyondDashDash: List<String>
    )

    fun analyzeArgv(argv: List<String>): Pair<Boolean, ArgvAnalysisResult?> {
        var state = ArgvState.NO_EXPECTATION
        var configFilePath: String? = null
        var withParentAccidents = false
        var withChildAccidents = false
        val discardedArgv = mutableListOf<String>() // the argvs from which we have removed what interests us
        val argsBeyondDashDash = mutableListOf<String>()
        for (arg in argv) {
            when (state) {
                ArgvState.NO_EXPECTATION -> {
                    if (arg == configOption) {
                        state = ArgvState.EXPECT_CONFIG_FILE
                    } else if (arg.startsWith("${configOption}=")) {
                        configFilePath = arg.substring("${configOption}=".length)
                        state = ArgvState.NO_EXPECTATION
                    } else if (arg == withChildAccidentsOption) {
                        withChildAccidents = true
                    } else if (arg == withParentAccidentsOption) {
                        withParentAccidents = true
                    } else if (arg == "--") {
                        state = ArgvState.BEYOND_DASH_DASH
                    } else {
                        discardedArgv.add(arg)
                    }
                }

                ArgvState.EXPECT_CONFIG_FILE -> {
                    configFilePath = arg
                    state = ArgvState.NO_EXPECTATION
                }

                ArgvState.BEYOND_DASH_DASH -> {
                    argsBeyondDashDash.add(arg)
                }
            }
        }
        //
        // We are erroring out right here if needed
        //
        if (state !in listOf(ArgvState.NO_EXPECTATION, ArgvState.BEYOND_DASH_DASH)) {
            Logging.logErr("The command line arguments are incomplete.", ParentMain.parentMarker)
            return Pair(false, null)
        }
        if (configFilePath == null) {
            Logging.logErr("No parent config file specified. Specify it with --config=<path>.", ParentMain.parentMarker)
            return Pair(false, null)
        }
        if (!discardedArgv.isEmpty()) {
            // This is not an error, but it's interesting to know what we have discarded'
            Logging.logWarn("Discarded arguments: ${discardedArgv.joinToString(", ")}", ParentMain.parentMarker)
        }
        Logging.logInfo("argvAnalysisResult: withChildAccidents = ${withChildAccidents}, discardedArgv = ${discardedArgv}, argsBeyondDashDash = ${argsBeyondDashDash}", parentMarker)
        return Pair(
            true,
            ArgvAnalysisResult(
                configFilePath,
                withParentAccidents,
                withChildAccidents,
                discardedArgv.toList(),
                argsBeyondDashDash.toList()
            )
        )
    }
}