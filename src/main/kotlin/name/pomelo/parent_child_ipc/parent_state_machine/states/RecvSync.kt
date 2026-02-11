package name.pomelo.parent_child_ipc.parent_state_machine.states

import name.pomelo.parent_child_ipc.state_machine_common.states.Recv
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData

class RecvSync(val managedParentData: ParentStateMachineData) : Recv(managedParentData) {

    data class ParseSyncResult(val timestamp: Long)

    companion object {

        private val regexSync = Regex("""SYNC:\s*([0-9]+)""")

        fun parseSync(line: String): ParseSyncResult? {
            val match = regexSync.matchEntire(line)
            if (match != null) {
                try {
                    return ParseSyncResult(match.groupValues[0].toLong())
                } catch (_: NumberFormatException) {
                    // failure
                }
            }
            return null
        }

    }

    override fun handleLine(line: String): StateDesc {
        val res = parseSync(line)
        if (res != null) {
            return SendSynAck(managedParentData.withSyncTimestamp(res.timestamp))
        } else {
            logErr("Unexpected line: '$line', resynchronizing", managedParentData.marker)
            return SendMisunderstood(managedParentData)
        }
    }

}
