package name.pomelo.parent_child_ipc.state_machine_common.states

import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData

abstract class StateDesc(val stateMachineData: StateMachineData) {

    abstract fun onEntry(): StateDesc
    open fun isFinal(): Boolean {
        return false
    }

}