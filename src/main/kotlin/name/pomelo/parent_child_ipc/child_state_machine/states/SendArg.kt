package name.pomelo.parent_child_ipc.child_state_machine.states

import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.Send
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString

class SendArg(val managedChildData: ChildStateMachineData) : Send(managedChildData) {

    override fun buildLine(): String {
        val arg = managedChildData.argv[managedChildData.argIndex]
        val mangledArg = mangleString(arg, -1, keepNewlines = false)
        return "ARG: ${managedChildData.argIndex} = '$mangledArg'"
    }

    override fun buildNextState(): StateDesc = RecvQuery(managedChildData.withIncArgIndex())

}