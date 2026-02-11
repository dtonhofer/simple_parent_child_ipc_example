package name.pomelo.parent_child_ipc.child_state_machine.states

import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.Send
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc

class SendReady(val managedChildData: ChildStateMachineData) : Send(managedChildData) {

    override fun buildLine(): String = "READY"
    override fun buildNextState(): StateDesc = RecvQuery(managedChildData.withZeroArgIndex())

}