package name.pomelo.parent_child_ipc.child_state_machine.states

import name.pomelo.parent_child_ipc.child_state_machine.ChildStateMachineData
import name.pomelo.parent_child_ipc.state_machine_common.states.Recv
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.state_machine_common.states.Abort
import name.pomelo.parent_child_ipc.state_machine_common.states.Blown
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString

class RecvSyncAck(val managedChildData: ChildStateMachineData) : Recv(managedChildData) {

    private val regex = Regex("""SYNC_ACK:\s*([0-9]+)""")
    private val syncCountMax = 3

    private fun parse(line: String): Boolean {
        val match = regex.matchEntire(line)
        if (match != null) {
            try {
                val timestamp = match.groupValues[1].toLong()
                if (timestamp == managedChildData.syncTimestamp) {
                    return true
                }
            } catch (_: NumberFormatException) {
                // failure
            }
        }
        return false
    }

    override fun handleLine(line: String): StateDesc {
        return if (parse(line)) {
            SendReady(managedChildData)
        } else if (line == "ABORT") {
            Abort(managedChildData)
        } else {
            val mangledLine = mangleString(line, -1, keepNewlines = false)
            logErr(this, "unexpected line '$mangledLine', resynchronizing with sync count = ${managedChildData.syncCount}", managedChildData.marker)
            if (managedChildData.syncCount < syncCountMax) {
                SendSync(managedChildData.withIncSyncCount())
            } else {
                logErr(this, "sync count maximum $syncCountMax reached", managedChildData.marker)
                Blown(managedChildData)
            }
        }
    }
}