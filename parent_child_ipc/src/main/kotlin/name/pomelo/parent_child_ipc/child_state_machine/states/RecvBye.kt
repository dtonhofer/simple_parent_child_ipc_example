package name.pomelo.parent_child_ipc.child_state_machine.states

import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.Abort
import name.pomelo.parent_child_ipc.state_machine_common.states.Done
import name.pomelo.parent_child_ipc.state_machine_common.states.Recv
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString

class RecvBye(val managedChildData: ChildStateMachineData) : Recv(managedChildData) {

    override fun handleLine(line: String): StateDesc {
        return if (line == "BYE") {
            Done(managedChildData)
        } else if (line == "ABORT") {
            Abort(managedChildData)
        } else {
            val mangledLine = mangleString(line, -1, keepNewlines = false)
            logErr(this, "unexpected line '$mangledLine', resynchronizing", managedChildData.marker)
            SendSync(managedChildData.withZeroSyncCount())
        }
    }
}