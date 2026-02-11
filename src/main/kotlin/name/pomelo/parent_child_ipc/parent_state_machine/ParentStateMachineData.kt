package name.pomelo.parent_child_ipc.parent_state_machine

import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData
import java.io.BufferedReader
import java.io.BufferedWriter

class ParentStateMachineData(
    reader: BufferedReader,
    writer: BufferedWriter,
    val syncTimestamp: Long,
    val argv: List<String>,
    marker: String
) : StateMachineData(reader, writer, marker) {

    // Constructor for the intitial data, i.e. the one given to the initial state
    constructor(reader: BufferedReader, writer: BufferedWriter, marker: String) : this(reader, writer, 0, listOf(), marker)

    fun withEmptyArgv() = ParentStateMachineData(reader, writer, syncTimestamp, listOf(), marker)
    fun withExtendedArgv(arg: String) = ParentStateMachineData(reader, writer, syncTimestamp, argv + listOf(arg), marker)

    fun withSyncTimestamp(freshSyncTimestamp: Long) = ParentStateMachineData(reader, writer, freshSyncTimestamp, argv, marker)
}