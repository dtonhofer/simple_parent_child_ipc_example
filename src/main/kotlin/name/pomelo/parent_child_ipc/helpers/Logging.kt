package name.pomelo.parent_child_ipc.helpers

import name.pomelo.parent_child_ipc.state_machine_common.states.StateDesc
import kotlin.math.max
import kotlin.text.iterator

object Logging {

    fun mangleMarker(marker: String): String {
        return if (marker.isNotEmpty()) {
            "[$marker]"
        } else {
            ""
        }
    }

    fun logErr(msg: String, ex: Exception, marker: String) {
        System.err.println("ERROR${mangleMarker(marker)}: $msg: ${ex.message}")
    }

    fun logErr(stateDesc: StateDesc, msg: String, ex: Exception, marker: String) {
        System.err.println("ERROR${mangleMarker(marker)}: ${stateDesc::class.simpleName}: $msg: ${ex.message}")
    }

    fun logErr(msg: String, marker: String) {
        System.err.println("ERROR${mangleMarker(marker)}: $msg")
    }

    fun logWarn(msg: String, marker: String) {
        System.err.println("WARN${mangleMarker(marker)}: $msg")
    }

    fun logErr(stateDesc: StateDesc, msg: String, marker: String) {
        System.err.println("ERROR${mangleMarker(marker)}: ${stateDesc::class.simpleName}: $msg")
    }

    fun logInfo(msg: String, marker: String) {
        System.err.println("INFO${mangleMarker(marker)}: $msg")
    }

    fun logInfo(msg: String) {
        System.err.println("INFO: $msg")
    }

    fun logInfo(stateDesc: StateDesc, msg: String, marker: String) {
        System.err.println("INFO${mangleMarker(marker)}: ${stateDesc::class.simpleName}: $msg")
    }

    fun logChildStderrLine(line: String) {
        // This is run by another thread
        // But: "You won’t get half a line from one thread glued to half a line from another
        // thread as long as each thread emits the line in one println call."
        System.err.println("CHILD STDERR: $line")
    }

    /**
     * Transform unprintables in a String into their hex representation, but add CRs to print on several lines
     * Passing null yields "(null)"
     */

    fun mangleString(str: String?, maxColumnCount: Int, keepNewlines: Boolean): String {
        val input = str ?: return "(null)"

        val effectiveMaxColumns = if (maxColumnCount <= 0) Int.MAX_VALUE else maxColumnCount
        val out = StringBuilder(input.length)

        var column = 0
        var pendingIndent = false

        fun isUnprintable(ch: Char): Boolean {
            val code = ch.code
            return (code < 0x20) || (code in 0x7F..0xA0) || (code > 0xFF)
        }

        fun appendEscaped(ch: Char): Int {
            val hex = Integer.toHexString(ch.code)
            out.append('[')
            if (hex.length == 1) out.append('0')
            out.append(hex)
            out.append(']')
            return 2 + max(2, hex.length)
        }

        for (ch in input) {
            if (column >= effectiveMaxColumns - 1 && (ch == ',' || ch == ' ')) {
                // try to break at a good place
                out.append(ch).append('\n')
                column = 0
                pendingIndent = true
                continue
            }

            if (pendingIndent) {
                out.append(INDENT)
                column = INDENT.length
                pendingIndent = false
            }

            if (keepNewlines && ch == '\n') {
                out.append(ch)
                column = 0
                continue
            }

            if (isUnprintable(ch)) {
                column += appendEscaped(ch)
            } else {
                out.append(ch)
                column++
            }
        }

        return out.toString()
    }

    private const val INDENT = "    "

}