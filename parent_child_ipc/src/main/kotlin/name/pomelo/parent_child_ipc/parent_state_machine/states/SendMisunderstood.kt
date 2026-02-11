package name.pomelo.parent_child_ipc.parent_state_machine.states

import name.pomelo.parent_child_ipc.state_machine_common.states.Send
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData


class SendMisunderstood(val managedParentData: ParentStateMachineData) : Send(managedParentData) {

    override fun buildLine(): String = "MISUNDERSTOOD" // any random string would do to start SYNCing
    override fun buildNextState(): StateDesc = RecvSync(managedParentData)

}