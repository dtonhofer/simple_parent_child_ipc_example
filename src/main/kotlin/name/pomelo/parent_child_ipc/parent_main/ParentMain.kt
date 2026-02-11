package name.pomelo.parent_child_ipc.parent_main

import name.pomelo.parent_child_ipc.helpers.CmdLineItem
import name.pomelo.parent_child_ipc.helpers.ConfigFileReader
import name.pomelo.parent_child_ipc.helpers.InterceptingWriter
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.helpers.Logging.logChildStderrLine
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachine
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object ParentMain {

    private val utf8 = Charsets.UTF_8
    private val parentMarker = "PARENT"

    private fun obtainProcessExitValue(process: Process): Int? {
        val maxAttemptCount = 10
        val waitTime_ms = 100
        //
        var value: Int? = null
        var countdown = maxAttemptCount
        //
        while (countdown > 0 && value == null) {
            try {
                value = process.exitValue()
            } catch (_: IllegalThreadStateException) {
                if (countdown > 1) {
                    logInfo("Child process hasn't exited yet - waiting $waitTime_ms ms.")
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                        countdown = 0
                        Thread.currentThread().interrupt()
                    }
                } else {
                    logInfo("Child process hasn't exited yet - giving up waiting.")
                }
            }
            countdown--
        }
        return value
    }

    private fun printConclusionOnManagedData(managedParentData: ParentStateMachineData) {
        if (managedParentData.argv.isEmpty()) {
            println("Child process has communicated 0 arguments.")
        } else {
            managedParentData.argv.forEachIndexed { index, arg -> println("argv[$index] = '$arg'") }
        }
    }

    private fun printConclusion(processExitValue: Int?, finalState: StateDesc) {
        println("Final parent state   : ${finalState::class.simpleName}")
        println("Child exit value     : ${processExitValue ?: "N/A"}")
        printConclusionOnManagedData(finalState.stateMachineData as ParentStateMachineData)
    }

    private fun buildProcessBuilderArgs(jarFile: String, args: List<CmdLineItem>): Array<String> {
        val cmdLineItems: MutableList<CmdLineItem> = mutableListOf()
        cmdLineItems.add(CmdLineItem("-jar", jarFile))
        cmdLineItems.addAll(args)
        val completeArgList = cmdLineItems.flatMap { it.toStringList() }
        return completeArgList.toTypedArray()
    }

    // https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/Process.html
    // https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/ProcessBuilder.html

    private fun buildJavaProcess(configData: ConfigData, vararg args: CmdLineItem): Process? {
        val (javaExe, jarFile, workDir) = configData
        val processBuilder = ProcessBuilder(javaExe, *buildProcessBuilderArgs(jarFile, args.toList()))
        processBuilder.directory(File(workDir))
        try {
            return processBuilder.start()
        } catch (ex: Exception) {
            logErr("Failed to start process", ex, parentMarker)
            return null
        }
    }

    private fun startThreadReadingChildStderr(errReader: BufferedReader) {
        // BufferedReader.readLine() returns the final line even if it doesn’t end
        // with \n (or \r\n). So if the child writes "oops" and then closes stderr,
        // your loop will still get "oops" once.
        thread(name = "Child stderr reader", isDaemon = true) {
            try {
                // Getting (null) here means the stream was closed.
                // In that case the line stil lcontains whatever was still available.
                generateSequence { errReader.readLine() }.forEach(::logChildStderrLine)
            } catch (_: IOException) {
                // Reader was closed somehow from our side: treat as normal shutdown
            } finally {
                logChildStderrLine("[[[ CHILD STDERR CLOSED ]]]")
            }
        }
    }

    private fun runParentProcessStateMachine(process: Process, reader: BufferedReader, writer: BufferedWriter): Pair<StateDesc, Int?> {
        val finalState = ParentStateMachine.run(
            ParentStateMachineData(reader, writer, parentMarker),
            reader,
            writer,
            withAccidents = false
        )
        // Obtaining the exit value may take a few milliseconds as we need to wait for the process to exit
        val processExitValue = obtainProcessExitValue(process)
        return Pair(finalState, processExitValue)
    }

    private fun communicateWithChildProcess(process: Process) {
        //
        // "process" is currently writing to "process.inputStream" (and blocking doing so)
        // Wrap a BufferedReader around that.
        //
        val reader: BufferedReader = process.inputReader(utf8)
        //
        // "process" is currently reading from "process.outputStream" (and blocking doing so)
        // Wrap a BufferedWriter around that.
        //
        val writer: BufferedWriter = process.outputWriter(utf8)
        //
        // "process" is currently logging to "process.errorStream" (and blocking doing so)
        // Wrap a BufferedReader around that.
        // Sadly, we can't use more file descriptors than the first three in Java.
        //
        val errReader: BufferedReader = process.errorReader(utf8)
        //
        // We just capture the child's output to its STDERR in a separate thread and print it to "our" STDERR
        //
        startThreadReadingChildStderr(errReader)
        //
        // In order to log messages sent from parent to child, we can intercept the "writer".
        //
        val interceptingWriter = InterceptingWriter(writer) {
            val mangledLine = mangleString(it, -1, false)
            logInfo("Parent to child: '$mangledLine'", parentMarker)
        }
        //
        // Now run the parent state machine until a final state is reached.
        //
        val (finalState, processExitValue) = runParentProcessStateMachine(process, reader, BufferedWriter(interceptingWriter))
        //
        // Then print conclusions. The "finalState" contains the data managed by the state machine
        // in the form of the last ManagedParentData instance created during the run.
        //
        printConclusion(processExitValue, finalState)
    }

    private enum class ArgvState { NO_EXPECTATION, EXPECT_CONFIG_FILE }

    private fun examineArgv(argv: Array<String>): String {
        var state = ArgvState.NO_EXPECTATION
        var configFilePath: String? = null
        for (arg in argv) {
            when (state) {
                ArgvState.NO_EXPECTATION -> {
                    if (arg == "--config") {
                        state = ArgvState.EXPECT_CONFIG_FILE
                    } else if (arg.startsWith("--config=")) {
                        configFilePath = arg.substring("--config=".length)
                        state = ArgvState.NO_EXPECTATION
                    } else {
                        error("Unexpected argument '$arg'.")
                    }
                }

                ArgvState.EXPECT_CONFIG_FILE -> {
                    configFilePath = arg
                    state = ArgvState.NO_EXPECTATION
                }
            }
        }
        if (state != ArgvState.NO_EXPECTATION) {
            error("The command line arguments are incomplete.")
        }
        if (configFilePath == null) {
            error("No config file specified. Specify it with --config=<path>.")
        }
        return configFilePath
    }

    private fun configFileValueNotFound(key: String, configFilePath: String): Nothing {
        error("A value for '$key' was not found in config file '$configFilePath'.")
    }

    data class ConfigData(val javaExe: String, val jarFile: String, val workDir: String)

    private fun findJarFileInConfig(configMap: Map<String, String>, configFilePath: String): String {
        val childJarFileKey = "childJarFile"
        val jarFile = configMap[childJarFileKey] ?: configFileValueNotFound(childJarFileKey, configFilePath)
        if (!File(jarFile).exists()) {
            error("Jar file '$jarFile' picked up from the config file '${configFilePath}' does not exist.")
        } else if (!File(jarFile).isFile) {
            error("Jar file '$jarFile' picked up from the config file '${configFilePath}' is not a file.")
        }
        return jarFile
    }

    private fun findJavaExeInConfig(configMap: Map<String, String>, configFilePath: String): String {
        val javaExeKey = "javaExe"
        val javaExe = configMap[javaExeKey] ?: configFileValueNotFound(javaExeKey, configFilePath)
        if (!File(javaExe).exists()) {
            error("Java executable '$javaExe' picked up from the config file '${configFilePath}' does not exist.")
        } else if (!File(javaExe).isFile || !File(javaExe).canExecute()) {
            error("Java executable '$javaExe' picked up from the config file '${configFilePath}' is not an executable file")
        }
        return javaExe
    }

    private fun findWorkDirInConfig(configMap: Map<String, String>, configFilePath: String): String {
        val workDirKey = "workDir"
        val workDir = configMap[workDirKey] ?: configFileValueNotFound(workDirKey, configFilePath)
        if (!File(workDir).exists()) {
            error("Work directory '$workDir' picked up from the config file '${configFilePath}' does not exist.")
        } else if (!File(workDir).isDirectory) {
            error("Work directory '$workDir' picked up from the config file '${configFilePath}' is not a directory.")
        }
        return workDir
    }

    private fun findConfigData(argv: Array<String>): ConfigData {
        //
        // Getting the config file path from the argv.
        //
        val configFilePath = examineArgv(argv) // may throw on error
        if (!File(configFilePath).exists()) {
            error("Config file '$configFilePath' does not exist")
        }
        if (!File(configFilePath).isFile) {
            error("Config file '$configFilePath' is not a regular file.")
        }
        //
        // Assuming this is a config file, load the data into a map
        // and then extract the three values we absolutely need.
        //
        val configMap = ConfigFileReader.loadConfigMap(configFilePath)
        val jarFile = findJarFileInConfig(configMap, configFilePath)
        val javaExe = findJavaExeInConfig(configMap, configFilePath)
        val workDir = findWorkDirInConfig(configMap, configFilePath)
        return ConfigData(javaExe, jarFile, workDir)
    }

    @JvmStatic
    fun main(argv: Array<String>) {
        val configData = findConfigData(argv)
        val process = buildJavaProcess(
            configData,
            CmdLineItem("A"),
            CmdLineItem("B"),
            CmdLineItem("C")
        )
        // The process is now running or null
        if (process != null) {
            communicateWithChildProcess(process)
        } else {
            exitProcess(1)
        }
    }
}

