package name.pomelo.parent_child_ipc.child_state_machine

import name.pomelo.parent_child_ipc.child_state_machine.states.RecvQuery
import name.pomelo.parent_child_ipc.helpers.Logging
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import kotlin.random.Random

object ChildStateMachine {

    private fun randomAccident(random: Random, smData: ChildStateMachineData) {
        val r = random.nextDouble()
        if (r in 0.95..1.0) {
            smData.closeReader()
        } else if (r in 0.85..0.9) {
            smData.closeWriter()
        } else if (r in 0.75..0.8) {
            smData.closeStreams()
        } else if (r in 0.65..0.7) {
            smData.writer.write("....line noise written to the child's writer....")
            smData.writer.newLine()
            smData.writer.flush()
        }
    }

    // ---
    // Run the "child" state machine until a final state is reached.
    // The descriptor for that state is then returned, allowing the caller to see what happened.
    // ---

    fun runStateMachine(smData: ChildStateMachineData, withAccidents: Boolean): StateDesc {
        val random = Random(System.currentTimeMillis())
        // We will start in state "RecvQuery" with certain "managed data"
        var stateDesc: StateDesc = RecvQuery(smData.withZeroArgIndex())
        while (!stateDesc.isFinal()) {
            Logging.logInfo("Entering state: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            stateDesc = stateDesc.onEntry()
            Logging.logInfo("Next state will be: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            // For testing, we may create "accidents" to simulate bugs
            if (withAccidents) {
                randomAccident(random, smData)
            }
        }
        return stateDesc
    }
}