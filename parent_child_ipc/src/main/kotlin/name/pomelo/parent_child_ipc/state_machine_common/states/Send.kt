package name.pomelo.parent_child_ipc.state_machine_common.states

import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString
import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData
import java.io.IOException

abstract class Send(stateMachineData: StateMachineData) : StateDesc(stateMachineData) {

    abstract fun buildLine(): String
    abstract fun buildNextState(): StateDesc

    override fun onEntry(): StateDesc {
        try {
            val line = buildLine()
            val mangledLine = mangleString(line, -1, keepNewlines = false)
            logInfo(this, "sending '$mangledLine'", stateMachineData.marker)
            stateMachineData.writer.write(line)
            stateMachineData.writer.newLine()
            stateMachineData.writer.flush()
            return buildNextState()
        } catch (ex: IOException) {
            logErr(this, "exception while writing line", ex, stateMachineData.marker)
            return Blown(stateMachineData)
        }
    }

}