package name.pomelo.parent_child_ipc.parent_state_machine.states

import name.pomelo.parent_child_ipc.state_machine_common.states.Recv
import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.parent_state_machine.ParentStateMachineData


class RecvAnswer(val managedParentData: ParentStateMachineData) : Recv(managedParentData) {

    data class ParseArgResult(val index: Int, val value: String)

    companion object {

        private val regexArg = Regex("""ARG:\s*([0-9]+)\s*=\s*'(.*)'""")

        private fun parseArg(line: String): ParseArgResult? {
            val match = regexArg.matchEntire(line)
            if (match != null) {
                try {
                    val theIndex = match.groupValues[1].toInt()
                    val theArg = match.groupValues[2]
                    return ParseArgResult(theIndex, theArg)
                } catch (_: NumberFormatException) {
                    // failure
                }
            }
            return null
        }

    }

    override fun handleLine(line: String): StateDesc {
        return if (line == "DONE") {
            SendBye(managedParentData)
        } else {
            val parseArgResult = parseArg(line)
            if (parseArgResult != null && parseArgResult.index == managedParentData.argv.size) {
                SendQuery(managedParentData.withExtendedArgv(parseArgResult.value))
            } else {
                val syncParseResult = RecvSync.parseSync(line)
                if (syncParseResult != null) {
                    SendSynAck(managedParentData.withSyncTimestamp(syncParseResult.timestamp))
                } else {
                    logErr("Unexpected line '$line', resynchronizing", managedParentData.marker)
                    SendMisunderstood(managedParentData)
                }
            }
        }
    }
}