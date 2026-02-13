package name.pomelo.parent_child_ipc

import name.pomelo.parent_child_ipc.configuration.ChildArgvAnalysis.analyzeArgv
import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachine
import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.helpers.Logging
import name.pomelo.parent_child_ipc.state_machine_common.states.Done
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import java.io.BufferedReader
import java.io.BufferedWriter

object ChildMain {

    const val childMarker = "CHILD"

    private fun runChildProcessStateMachine(argvToTransmit: List<String>, reader: BufferedReader, writer: BufferedWriter, withAccidents: Boolean): StateDesc {
        val smData = ChildStateMachineData(argvToTransmit, reader, writer, childMarker)
        val childFinalState = ChildStateMachine.runStateMachine(smData, withAccidents)
        Logging.logInfo("Final state: ${childFinalState::class.simpleName}", smData.marker)
        assert(childFinalState.isFinal())
        return childFinalState
    }

    fun goIntoChildMode(argv: List<String>): Int {
        val (itWorked, argvAnalysisResult) = analyzeArgv(argv)
        if (!itWorked) {
            return 1
        }
        val reader = System.`in`.bufferedReader(Charsets.UTF_8)
        val writer = System.out.bufferedWriter(Charsets.UTF_8)
        val withAccidents = argvAnalysisResult!!.withChildAccidents
        val argvToTransmit = argvAnalysisResult.argsBeyondDashDash
        val childFinalState = runChildProcessStateMachine(argvToTransmit, reader, writer, withAccidents)
        if (childFinalState is Done) {
            return 0
        } else {
            return 1
        }
    }
}