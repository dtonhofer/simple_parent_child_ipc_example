package name.pomelo.parent_child_ipc.parent_state_machine.states

import name.pomelo.parent_child_ipc.state_machine_common.states.Recv
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData

class RecvReady(val managedParentData: ParentStateMachineData) : Recv(managedParentData) {

    override fun handleLine(line: String): StateDesc {
        if (line == "READY") {
            return SendQuery(managedParentData.withEmptyArgv())
        } else {
            logErr("Unexpected line '$line', resynchronizing", managedParentData.marker)
            return SendMisunderstood(managedParentData)
        }
    }
}