package name.pomelo.parent_child_ipc

import name.pomelo.parent_child_ipc.configuration.MainArgvAnalysis.analyzeArgv
import name.pomelo.parent_child_ipc.configuration.MainArgvAnalysis.printHelp
import name.pomelo.parent_child_ipc.helpers.Logging
import kotlin.system.exitProcess

object Main {

    const val mainMarker = "MAIN"

    enum class Mode { PARENT, CHILD }

    // ---
    // The main entry point!
    // > To create the main entry point:
    // > Either have main() in an object and mark it as @JvmStatic or have a top-level function and call it directly.
    // > This generates a separate *Kt class with a static main, which many build tools/launchers handle very predictably.
    // ---

    @JvmStatic
    fun main(argv: Array<String>) {
        val (itWorked, analysisResult) = analyzeArgv(argv.toList())
        if (!itWorked) {
            Logging.logErr("Failed to successfully analyze arguments.", mainMarker)
            exitProcess(2)
        } else {
            assert(analysisResult != null)
            if (analysisResult!!.printHelp) {
                printHelp()
                exitProcess(2)
            } else {
                val exitValue = when (analysisResult.mode) {
                    Mode.PARENT -> ParentMain.goIntoParentMode(analysisResult.discardedArgv)
                    Mode.CHILD -> ChildMain.goIntoChildMode(analysisResult.discardedArgv)
                    null -> error("This should never happen.")
                }
                exitProcess(exitValue)
            }
        }
    }
}

