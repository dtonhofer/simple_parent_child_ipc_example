package name.pomelo.parent_child_ipc

import name.pomelo.parent_child_ipc.configuration.ParentConfigFileAnalysis
import name.pomelo.parent_child_ipc.configuration.ParentConfigFileAnalysis.analyzeArgvAndFindConfigData
import name.pomelo.parent_child_ipc.helpers.CmdLineItem
import name.pomelo.parent_child_ipc.helpers.InterceptingWriter
import name.pomelo.parent_child_ipc.helpers.Logging
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachine
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

object ParentMain {

    private val utf8 = Charsets.UTF_8
    const val parentMarker = "PARENT"

    private fun obtainProcessExitValue(process: Process): Int? {
        val maxAttemptCount = 10
        val waitTime_ms = 10
        //
        var value: Int? = null
        var countdown = maxAttemptCount
        //
        while (countdown > 0 && value == null) {
            try {
                value = process.exitValue()
            } catch (_: IllegalThreadStateException) {
                if (countdown > 1) {
                    Logging.logInfo("Child process hasn't exited yet - waiting $waitTime_ms ms.")
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                        countdown = 0
                        Thread.currentThread().interrupt()
                    }
                } else {
                    Logging.logInfo("Child process hasn't exited yet - giving up waiting.")
                }
            }
            countdown--
        }
        return value
    }

    private fun printConclusionOnManagedData(parentSmData: ParentStateMachineData) {
        if (parentSmData.argv.isEmpty()) {
            println("Child process has communicated 0 arguments.")
        } else {
            parentSmData.argv.forEachIndexed { index, arg -> println("argv[$index] = '$arg'") }
        }
    }

    private fun printConclusion(pccr: ParentChildCommunicationResult) {
        println("Final parent state   : ${pccr.parentFinalState::class.simpleName}")
        println("Child exit value     : ${pccr.childExitValue ?: "N/A"}")
        val parentSmData = pccr.parentFinalState.stateMachineData as ParentStateMachineData
        printConclusionOnManagedData(parentSmData)
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

    private fun buildJavaProcess(configData: ParentConfigFileAnalysis.ConfigData): Process {
        // No extra processing, just sheer values
        val args = mutableListOf<CmdLineItem>()
        args.add(CmdLineItem("--child"))
        if (configData.withChildAccidents) {
            args.add(CmdLineItem("--with-child-accidents"))
        }
        if (!configData.argsBeyondDashDash.isEmpty()) {
            args.add(CmdLineItem("--"))
            args.addAll(configData.argsBeyondDashDash.map { CmdLineItem(it) }.toList())
        }
        val jarFile = configData.jarFile
        val javaExe = configData.javaExe
        val workDir = configData.workDir
        val processBuilder = ProcessBuilder(javaExe, *buildProcessBuilderArgs(jarFile, args))
        processBuilder.directory(File(workDir))
        return processBuilder.start()
    }

    private fun startThreadReadingChildStderr(errReader: BufferedReader) {
        // BufferedReader.readLine() returns the final line even if it doesn’t end
        // with \n (or \r\n). So if the child writes "oops" and then closes stderr,
        // your loop will still get "oops" once.
        thread(name = "Child stderr reader", isDaemon = true) {
            try {
                // Getting (null) here means the stream was closed.
                // In that case the line stil lcontains whatever was still available.
                generateSequence { errReader.readLine() }.forEach(Logging::logChildStderrLine)
            } catch (_: IOException) {
                // Reader was closed somehow from our side: treat as normal shutdown
            } finally {
                Logging.logChildStderrLine("[[[ CHILD STDERR CLOSED ]]]")
            }
        }
    }

    private data class ParentChildCommunicationResult(val parentFinalState: StateDesc, val childExitValue: Int?)

    private fun runParentProcessStateMachine(childProcess: Process, reader: BufferedReader, writer: BufferedWriter, withAccidents: Boolean): ParentChildCommunicationResult {
        val smData = ParentStateMachineData(reader, writer, parentMarker)
        val parentFinalState = ParentStateMachine.runStateMachine(smData, withAccidents)
        Logging.logInfo("Final state: ${parentFinalState::class.simpleName}", smData.marker)
        assert(parentFinalState.isFinal())
        // Obtaining the exit value may take a few milliseconds as we need to wait for the process to exit
        val childExitValue = obtainProcessExitValue(childProcess)
        return ParentChildCommunicationResult(parentFinalState, childExitValue)
    }

    private fun communicateWithChildProcess(process: Process, withAccidents: Boolean): ParentChildCommunicationResult {
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
            val mangledLine = Logging.mangleString(it, -1, false)
            Logging.logInfo("Parent to child: '$mangledLine'", parentMarker)
        }
        //
        // Now run the parent state machine until a final state is reached.
        //
        // >>>>
        return runParentProcessStateMachine(
            process,
            reader,
            BufferedWriter(interceptingWriter),
            withAccidents
        )
        // <<<<
    }


    fun goIntoParentMode(argv: List<String>): Int {
        val (itWorked, configData) = analyzeArgvAndFindConfigData(argv)
        if (!itWorked) {
            return 1
        }
        var childProcess: Process?
        try {
            childProcess = buildJavaProcess(configData!!)
        } catch (ex: Exception) {
            Logging.logErr("Failed to start process", ex, parentMarker)
            return 1
        }
        assert(childProcess != null)
        assert(configData != null)
        //
        // The process is now running. Do the exchange.
        //
        // >>>>>
        val pccr = communicateWithChildProcess(childProcess, configData.withParentAccidents)
        assert(pccr.parentFinalState.isFinal())
        // <<<<<
        //
        // Then print conclusions. The "finalState" contains the data managed by the state machine
        // in the form of the last ManagedParentData instance created during the run.
        //
        printConclusion(pccr)
        //
        // If pccr.childProcessExitValue == null, the next line returns 1 because null != 0
        //
        return (if (pccr.childExitValue == 0) 0 else 1)
    }
}

