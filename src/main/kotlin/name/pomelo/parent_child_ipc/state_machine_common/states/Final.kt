package name.pomelo.parent_child_ipc.state_machine_common.states

import name.pomelo.parent_child_ipc.state_machine_common.StateMachineData

abstract class Final(stateMachineData: StateMachineData) : StateDesc(stateMachineData) {

    private var canBeCalled = true

    // ---
    // Anything derived from this class is automatically "final".
    // "isFinal()" could actually test whether the instance is a subclass of StateDescFinal.
    // ---

    override fun isFinal(): Boolean {
        return true
    }

    // ---
    // onEntry() just closes the stream, then returns its own instance (as it cannot return null).
    // (in "uncallable" method make doubly sure)
    // ---

    override fun onEntry(): StateDesc {
        if (!canBeCalled) {
            // Do not call to avoid infinite loops in the runner if there is a programming error.
            error("This state descriptor cannot be called again")
        } else {
            stateMachineData.closeStreams()
            canBeCalled = false
            return this
        }
    }

}