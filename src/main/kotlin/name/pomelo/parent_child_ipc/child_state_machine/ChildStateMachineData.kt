package name.pomelo.parent_child_ipc.child_state_machine

import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData
import java.io.BufferedReader
import java.io.BufferedWriter

class ChildStateMachineData(
    val argIndex: Int, // index into argv, counting up from 0 as the state machine progresses
    val argvToTransmit: List<String>, // the argument vector received in main(); we send the individual elements to the "parent process"
    reader: BufferedReader, // reader on top of STDIN, the requests from the "parent process" are read from it
    writer: BufferedWriter, // reader on top of STDOUT, the responses to the "parent process" are written to it
    val syncTimestamp: Long, // when synchronization with the "parent process" happens, the timestamp is temporarily stored here
    val syncCount: Int, // when synchronization with the "parent process" happens, this counter avoids infinite loops
    marker: String // a marker used when printing log messages
) : StateMachineData(reader, writer, marker) {

    // Constructor for the intitial data, i.e. the one given to the initial state
    constructor(argvToTransmit: List<String>, reader: BufferedReader, writer: BufferedWriter, marker: String) : this(0, argvToTransmit, reader, writer, 0, 0, marker)

    fun withIncArgIndex() = ChildStateMachineData(argIndex + 1, argvToTransmit, reader, writer, syncTimestamp, syncCount, marker)
    fun withZeroArgIndex() = ChildStateMachineData(0, argvToTransmit, reader, writer, syncTimestamp, syncCount, marker)

    fun withSyncTimestamp(freshSyncTimestamp: Long) = ChildStateMachineData(argIndex, argvToTransmit, reader, writer, freshSyncTimestamp, syncCount, marker)

    fun withZeroSyncCount() = ChildStateMachineData(argIndex, argvToTransmit, reader, writer, syncTimestamp, 0, marker)
    fun withIncSyncCount() = ChildStateMachineData(argIndex, argvToTransmit, reader, writer, syncTimestamp, syncCount + 1, marker)

}