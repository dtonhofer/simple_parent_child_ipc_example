package name.pomelo.parent_child_ipc.configuration

import name.pomelo.parent_child_ipc.ChildMain
import name.pomelo.parent_child_ipc.helpers.Logging

object ChildArgvAnalysis {

    private const val withChildAccidentsOption = "--with-child-accidents" // specific to parent

    private enum class ArgvState { NO_EXPECTATION, BEYOND_DASH_DASH }

    data class ArgvAnalysisResult(
        val withChildAccidents: Boolean,
        val discardedArgv: List<String>,
        val argsBeyondDashDash: List<String>
    )

    fun analyzeArgv(argv: List<String>): Pair<Boolean, ArgvAnalysisResult?> {
        var state = ArgvState.NO_EXPECTATION
        var withChildAccidents = false
        val discardedArgv = mutableListOf<String>() // the argvs from which we have removed what interests us
        val argsBeyondDashDash = mutableListOf<String>()
        for (arg in argv) {
            when (state) {
                ArgvState.NO_EXPECTATION -> {
                    if (arg == withChildAccidentsOption) {
                        withChildAccidents = true
                    } else if (arg == "--") {
                        state = ArgvState.BEYOND_DASH_DASH
                    } else {
                        discardedArgv.add(arg)
                    }
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
            Logging.logErr("The command line arguments are incomplete.", ChildMain.childMarker)
            return Pair(false, null)
        }
        if (!discardedArgv.isEmpty()) {
            // This is not an error, but it's interesting to know what we have discarded'
            Logging.logWarn("Discarded arguments: ${discardedArgv.joinToString(", ")}", ChildMain.childMarker)
        }
        Logging.logInfo("argvAnalysisResult: withChildAccidents = ${withChildAccidents}, discardedArgv = ${discardedArgv}, argsBeyondDashDash = ${argsBeyondDashDash}", ChildMain.childMarker)
        return Pair(
            true,
            ArgvAnalysisResult(
                withChildAccidents,
                discardedArgv.toList(),
                argsBeyondDashDash.toList()
            )
        )
    }

}