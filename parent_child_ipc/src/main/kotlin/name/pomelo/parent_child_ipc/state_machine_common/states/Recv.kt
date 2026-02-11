package name.pomelo.parent_child_ipc.state_machine_common.states

import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import name.pomelo.parent_child_ipc.helpers.Logging.mangleString
import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData
import java.io.IOException

abstract class Recv(stateMachineData: StateMachineData) : StateDesc(stateMachineData) {

    abstract fun handleLine(line: String): StateDesc

    override fun onEntry(): StateDesc {
        try {
            // If there is a timeout reading on the lower layers, there will probably be an IOException here.
            // What happens if the stream closes in the middle of a multibyte character?
            val line = stateMachineData.reader.readLine()
            val mangledLine = mangleString(line, -1, keepNewlines = false)
            logInfo(this, "received '$mangledLine'", stateMachineData.marker)
            if (line == null) {
                // EOF was reached without reading any characters
                return Blown(stateMachineData)
            } else {
                // Something was read.
                // (and maybe EOF was reached, but we won't test that right now)
                // Ask the handler implemented in the subclass to process the line.
                return handleLine(line)
            }
        } catch (ex: IOException) {
            logErr(this, "exception while reading line", ex, stateMachineData.marker)
            return Blown(stateMachineData)
        }
    }

}