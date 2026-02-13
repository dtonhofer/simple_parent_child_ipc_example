# simple_parent_child_ipc_example

## What is this?

An exercise in Kotlin to have a 'parent process' start a 'child process', with the 'parent process' then talking with the 'child process' via piped STDIN/STDOUT.

Here is the general idea concerning the architecture:

![parent child configuration](images/parent_child_configuration.png)

## Features

- Argv processing for the parent and child process is based on locally programmed-out finite state machines.
  The alternative would have been to use an external library, which would have allowed a more declarative approach.
  However, argv processing is simple enough and Kotlin is expressive enough that we can bake specialized code directly.
- Parent process and child process behaviour are based on finite state machines that send one-liners to each other over piped STDIN/STDOUT.
  Implementation-wise, "states" are instances from a class hierarchy, each non-abstract class standing for a possible state. The class
  hierarchy allows factoring out common behavior into superclasses, and also to partition the set of states into the partition of "final states",
  "states during which the process listens for data on STDIN", "states during which the process listens for data on STDOUT" etc. 
  An instance representing a state exists for a single step of the finite state machine. It is thrown away and replaced by a fresh instance
  representing the follower state when, conceptually, "edge traversal" happens. Ever "state class" has an `onEntry()` method which is called
  by the state machine runner (a very small loop of 3 lines) immediately after the state instance has been created. Any actions that happen
  "during that state" (a fuzzy concept, really, as it may not be immediately clear when a state conceptually "starts" or "ends", one has to
  make some choices that may not be in accord with formalisms like UML) is performed in the `onEntry()` method. In particular, receiving
  a line from STDIN (blocking as long has it hasn't fully arrived), sending a line to STDOUT (blocking if the output buffer is full) and
  deciding what shall be the next state (same as deciding what outgoing edge to travers). `onEntry()` simple returns the next state instance
  to the state machine runner, which will then call `onEntry()` on it, and the cycle repeats. Any ancillary data (counters, streams) that the
  state machine needs to handle in addition to the central representation of in what state it is now is kept in dedicated immutable instances
  references by the state instances. If the ancillary data changes (e.g. there is a counter incrementation), then a fresh anciallary data
  instance based on the existing one is created and given to the next state instance's constructor. This yields a rather elegant solution, code-wise. 

## The parent state machine

![parent state machine](images/parent_process_state_machine.png)

### The child state machine

![child state machine](images/child_process_state_machine.png)

## The class hierarchy of states

![class hierarchy of states](images/class_hierarchy_of_states.png)

## Notes

- Everything is in the namespace `name.pomelo.parent_child_ipc`

## How to run this

- Compile an "überjar" by running the maven target "clean install". 
- Edit the configuration file, here named "config.txt", indicating the "überjar" just created, the Java VM executable (as the POM says to compile to version 21,
  the java VM must be at least version 21) and the working directory for the child process (see below for an example)
- Start the parent process, indicating:
  - The config file by giving `--config=<path/to/config/file>` as option
  - Whether you want or generate random "accidents" in the 
- You will see the log whereby the parent process asks the child sequentially for its arguments and receives the `A`, `B`, `C` in turn, finally printing the received string vector to STDOUT.

Start the parent-child pair. The strings "A", "B", "C" will be communicated from the child process to the parent process over piped I/O:

java -jar ~/simple_parent_child_ipc_example/target/parent_child_ipc-1.0.jar --config=~/simple_parent_child_ipc_example/config.txt -- A B C

As above, but there will be "accidents" (random closing of STDIN or STDOUT and unexpected strings in the exchange between parent and child, for testing purposes)

java -jar ~/simple_parent_child_ipc_example/target/parent_child_ipc-1.0.jar --config=~/simple_parent_child_ipc_example/config.txt --with-child-accidents --with-parent-accident -- A B C

You can run the child in isolation and talk to it from the console. Consult the child state machine diagram to learn how to perform the exchange.

java -jar ~/simple_parent_child_ipc_example/target/parent_child_ipc-1.0.jar --config=~/simple_parent_child_ipc_example/config.txt --child

## An example run

Running:

```
java -jar ~/simple_parent_child_ipc_example/target/parent_child_ipc-1.0.jar --config=~/simple_parent_child_ipc_example/config.txt -- A B C
```

We get this:

```
INFO[PARENT]: argvAnalysisResult: withChildAccidents = false, discardedArgv = [], argsBeyondDashDash = [A, B, C]
INFO[PARENT]: Entering state: SendQuery
INFO[PARENT]: SendQuery: sending 'ARG?'
INFO[PARENT]: Parent to child: 'ARG?[0a]'
INFO[PARENT]: Next state will be: RecvAnswer
INFO[PARENT]: Entering state: RecvAnswer
CHILD STDERR: INFO[CHILD]: argvAnalysisResult: withChildAccidents = false, discardedArgv = [], argsBeyondDashDash = [A, B, C]
CHILD STDERR: INFO[CHILD]: Entering state: RecvQuery
CHILD STDERR: INFO[CHILD]: RecvQuery: received 'ARG?'
CHILD STDERR: INFO[CHILD]: Next state will be: SendArg
CHILD STDERR: INFO[CHILD]: Entering state: SendArg
CHILD STDERR: INFO[CHILD]: SendArg: sending 'ARG: 0 = 'A''
CHILD STDERR: INFO[CHILD]: Next state will be: RecvQuery
CHILD STDERR: INFO[CHILD]: Entering state: RecvQuery
INFO[PARENT]: RecvAnswer: received 'ARG: 0 = 'A''
INFO[PARENT]: Next state will be: SendQuery
INFO[PARENT]: Entering state: SendQuery
INFO[PARENT]: SendQuery: sending 'ARG?'
INFO[PARENT]: Parent to child: 'ARG?[0a]'
INFO[PARENT]: Next state will be: RecvAnswer
INFO[PARENT]: Entering state: RecvAnswer
CHILD STDERR: INFO[CHILD]: RecvQuery: received 'ARG?'
CHILD STDERR: INFO[CHILD]: Next state will be: SendArg
CHILD STDERR: INFO[CHILD]: Entering state: SendArg
CHILD STDERR: INFO[CHILD]: SendArg: sending 'ARG: 1 = 'B''
INFO[PARENT]: RecvAnswer: received 'ARG: 1 = 'B''
CHILD STDERR: INFO[CHILD]: Next state will be: RecvQuery
CHILD STDERR: INFO[CHILD]: Entering state: RecvQuery
INFO[PARENT]: Next state will be: SendQuery
INFO[PARENT]: Entering state: SendQuery
INFO[PARENT]: SendQuery: sending 'ARG?'
INFO[PARENT]: Parent to child: 'ARG?[0a]'
INFO[PARENT]: Next state will be: RecvAnswer
INFO[PARENT]: Entering state: RecvAnswer
CHILD STDERR: INFO[CHILD]: RecvQuery: received 'ARG?'
CHILD STDERR: INFO[CHILD]: Next state will be: SendArg
CHILD STDERR: INFO[CHILD]: Entering state: SendArg
CHILD STDERR: INFO[CHILD]: SendArg: sending 'ARG: 2 = 'C''
INFO[PARENT]: RecvAnswer: received 'ARG: 2 = 'C''
CHILD STDERR: INFO[CHILD]: Next state will be: RecvQuery
CHILD STDERR: INFO[CHILD]: Entering state: RecvQuery
INFO[PARENT]: Next state will be: SendQuery
INFO[PARENT]: Entering state: SendQuery
INFO[PARENT]: SendQuery: sending 'ARG?'
INFO[PARENT]: Parent to child: 'ARG?[0a]'
INFO[PARENT]: Next state will be: RecvAnswer
INFO[PARENT]: Entering state: RecvAnswer
CHILD STDERR: INFO[CHILD]: RecvQuery: received 'ARG?'
CHILD STDERR: INFO[CHILD]: Next state will be: SendDone
CHILD STDERR: INFO[CHILD]: Entering state: SendDone
INFO[PARENT]: RecvAnswer: received 'DONE'
CHILD STDERR: INFO[CHILD]: SendDone: sending 'DONE'
INFO[PARENT]: Next state will be: SendBye
CHILD STDERR: INFO[CHILD]: Next state will be: RecvBye
CHILD STDERR: INFO[CHILD]: Entering state: RecvBye
INFO[PARENT]: Entering state: SendBye
INFO[PARENT]: SendBye: sending 'BYE'
INFO[PARENT]: Parent to child: 'BYE[0a]'
CHILD STDERR: INFO[CHILD]: RecvBye: received 'BYE'
INFO[PARENT]: Next state will be: Done
INFO[PARENT]: Final state: Done
CHILD STDERR: INFO[CHILD]: Next state will be: Done
CHILD STDERR: INFO[CHILD]: Final state: Done
INFO: Child process hasn't exited yet - waiting 10 ms.
CHILD STDERR: [[[ CHILD STDERR CLOSED ]]]
Final parent state   : Done
Child exit value     : 0
argv[0] = 'A'
argv[1] = 'B'
argv[2] = 'C'
```

### Example config file

```
####
# Configuration that tells the parent process how to start the child process.
# Where this file is (it not being a resource) is communicted to the parent
# process with the --config=<path> option.
#
# The config file reader understands $HOME and ~ as shorthand for the user's home directory.
####

# The directory in which the child process will be started.

workDir = $HOME

# The uberjar which contains all the code for the child process.
# That uberjar is created with the aven target "clean install"

childJarFile = $HOME/simple_parent_child_ipc_example/target/parent_child_ipc-1.0.jar

# The Java executable used to run the child process.
# Asuming at least version 21 because that is the target bytecode version
# stipulated in the pom.xml

javaExe      = /usr/local/java/jdk22_64_adopt/bin/java```
```




