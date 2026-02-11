package name.pomelo.parent_child_ipc.child_state_machine.states

import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.Send
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc

class SendSync(val managedChildData: ChildStateMachineData) : Send(managedChildData) {

    private val syncTimestamp = System.currentTimeMillis()

    override fun buildLine(): String = "SYNC: $syncTimestamp"
    override fun buildNextState(): StateDesc = RecvSyncAck(managedChildData.withSyncTimestamp(syncTimestamp))

}