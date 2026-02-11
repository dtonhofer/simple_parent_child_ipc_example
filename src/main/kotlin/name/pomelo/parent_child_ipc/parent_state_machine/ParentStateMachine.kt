package name.pomelo.parent_child_ipc.parent_state_machine

import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import name.pomelo.parent_child_ipc.parent_state_machine.states.SendQuery
import java.io.BufferedReader
import java.io.BufferedWriter
import kotlin.random.Random

object ParentStateMachine {

    private fun randomAccident(random: Random, managedParentData: ParentStateMachineData) {
        val r = random.nextDouble()
        if (r in 0.95..1.0) {
            managedParentData.closeReader()
        } else if (r in 0.85..0.9) {
            managedParentData.closeWriter()
        } else if (r in 0.75..0.8) {
            managedParentData.closeStreams()
        } else if (r in 0.65..0.7) {
            managedParentData.writer.write("....line noise written to the parent's writer....")
            managedParentData.writer.newLine()
            managedParentData.writer.flush()
        }
    }

    fun run(managedParentData: ParentStateMachineData, reader: BufferedReader, writer: BufferedWriter, withAccidents: Boolean): StateDesc {
        val random = Random(System.currentTimeMillis())
        // We will start in state "SendQuery" with certain "managed parent data"
        var stateDesc: StateDesc = SendQuery(managedParentData)
        while (!stateDesc.isFinal()) {
            logInfo("Entering state: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            stateDesc = stateDesc.onEntry()
            logInfo("Next state will be: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            // For testing, we may create "accidents" to simulate bugs
            if (withAccidents) {
                randomAccident(random, stateDesc.stateMachineData as ParentStateMachineData)
            }
        }
        return stateDesc
    }

}