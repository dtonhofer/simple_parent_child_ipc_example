package name.pomelo.parent_child_ipc.configuration

import name.pomelo.parent_child_ipc.Main
import name.pomelo.parent_child_ipc.helpers.Logging

object MainArgvAnalysis {

    private enum class ArgvState { NO_EXPECTATION, FLUSH_REMAINDER }

    private const val HELP_OPTION_LONG = "--help"
    private const val HELP_OPTION_SHORT = "-h"
    private const val CHILD_MODE_OPTION = "--child"
    private const val DOUBLE_DASH = "--"

    data class ArgvAnalysisResult(val mode: Main.Mode?, val printHelp: Boolean, val discardedArgv: List<String>)

    fun analyzeArgv(argv: List<String>): Pair<Boolean, ArgvAnalysisResult?> {
        var state = ArgvState.NO_EXPECTATION
        var mode: Main.Mode = Main.Mode.PARENT // default
        var askForHelp = false
        val discardedArgv = mutableListOf<String>() // the argvs from which we have removed what interests us
        for (arg in argv) {
            when (state) {
                ArgvState.NO_EXPECTATION -> {
                    when (arg) {
                        CHILD_MODE_OPTION -> {
                            mode = Main.Mode.CHILD
                            state = ArgvState.NO_EXPECTATION
                        }
                        HELP_OPTION_LONG, HELP_OPTION_SHORT -> {
                            askForHelp = true
                            state = ArgvState.NO_EXPECTATION
                        }
                        DOUBLE_DASH -> {
                            // "--" means that all remaining args are not relevant to us
                            state = ArgvState.FLUSH_REMAINDER
                            discardedArgv.add(arg)
                        }
                        else -> {
                            discardedArgv.add(arg)
                        }
                    }
                }

                ArgvState.FLUSH_REMAINDER -> {
                    // Beyond the "--" just add everything to "reducedArgv"
                    discardedArgv.add(arg)
                }
            }
        }
        return if (askForHelp) {
            // Do not give the mode but ask for help
            Pair(true, ArgvAnalysisResult(null, true, discardedArgv))
        } else if (state !in listOf(ArgvState.NO_EXPECTATION, ArgvState.FLUSH_REMAINDER)) {
            // This actually never happens with the few arguments we have
            Logging.logErr("The arguments are incomplete.", "MAIN")
            Pair(false, null)
        } else {
            Pair(true, ArgvAnalysisResult(mode, false, discardedArgv))
        }
    }

    fun printHelp() {
        println("Usage: java -jar name_of_the_jar_file.jar [--mode=<mode>] [--help] [...more args depending on mode...]")
        println("  --help or -h             Print this help message.")
        println("  --child                  Do not run in default parent mode, but in child mode.")
        println("  --config=<path>          Indicate the path to the config file (parent mode only).")
        println("  --with-parent-accidents  Run the parent with 'processing accidents' for testing purposes (parent mode only).")
        println("  --with-child-accidents   Run the child with 'processing accidents' for testing purposes (parent and child mode).")
        println("  -- [arg...]              Any arguments after '--' will be returned to the parent as they are via pipe I/O.")
    }
}