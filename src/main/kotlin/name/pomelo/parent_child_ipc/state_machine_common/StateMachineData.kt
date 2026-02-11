package name.pomelo.parent_child_ipc.state_machine_common

import name.pomelo.parent_child_ipc.helpers.Logging.logErr
import java.io.BufferedReader
import java.io.BufferedWriter

open class StateMachineData(
    val reader: BufferedReader,
    val writer: BufferedWriter,
    val marker: String // this marker is used when writing to the log
) {

    fun closeReader() {
        try {
            reader.close()
        } catch (ex: Exception) {
            logErr("Could not cleanly close reader", ex, marker)
        }
    }

    fun closeWriter() {
        try {
            writer.close()
        } catch (ex: Exception) {
            logErr("Could not cleanly close writer", ex, marker)
        }
    }

    fun closeStreams() {
        closeReader()
        closeWriter()
    }
}