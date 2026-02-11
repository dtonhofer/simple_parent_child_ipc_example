package name.pomelo.parent_child_ipc.child_state_machine

import name.pomelo.parent_child_ipc.child_state_machine.states.RecvQuery
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import kotlin.random.Random

object ChildMain {

    private fun randomAccident(random: Random, managedChildData: ChildStateMachineData) {
        val r = random.nextDouble()
        if (r in 0.95..1.0) {
            managedChildData.closeReader()
        } else if (r in 0.85..0.9) {
            managedChildData.closeWriter()
        } else if (r in 0.75..0.8) {
            managedChildData.closeStreams()
        } else if (r in 0.65..0.7) {
            managedChildData.writer.write("....line noise written to the child's writer....")
            managedChildData.writer.newLine()
            managedChildData.writer.flush()
        }
    }

    // ---
    // Run the "child" state machine until a final state is reached.
    // The descriptor for that state is then returned, allowing the caller to see what happened.
    // ---

    private fun runChildStateMachine(managedChildData: ChildStateMachineData, withAccidents: Boolean): StateDesc {
        val random = Random(System.currentTimeMillis())
        // We will start in state "RecvQuery" with certain "managed data"
        var stateDesc: StateDesc = RecvQuery(managedChildData.withZeroArgIndex())
        while (!stateDesc.isFinal()) {
            logInfo("Entering state: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            stateDesc = stateDesc.onEntry()
            logInfo("Next state will be: ${stateDesc::class.simpleName}", stateDesc.stateMachineData.marker)
            // For testing, we may create "accidents" to simulate bugs
            if (withAccidents) {
                randomAccident(random, managedChildData)
            }
        }
        return stateDesc
    }

    // ---
    // To create the main entry point:
    // Either have main() in an object and mark it as @JvmStatic or have a top-level function and
    // call it directly.
    // This generates a separate *Kt class with a static main, which many build tools/launchers
    // handle very predictably.
    // ---

    @JvmStatic
    fun main(argv: Array<String>) {
        val reader = System.`in`.bufferedReader(Charsets.UTF_8)
        val writer = System.out.bufferedWriter(Charsets.UTF_8)
        val childData = ChildStateMachineData(0, argv.toList(), reader, writer, 0, 0, "CHILD")
        val finalState = runChildStateMachine(childData, withAccidents = false)
        logInfo("Final state: ${finalState::class.simpleName}", childData.marker)
        assert(finalState.isFinal())
    }

}