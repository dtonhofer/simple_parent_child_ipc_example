package name.pomelo.parent_child_ipc.helpers

import java.io.Writer

class InterceptingWriter(private val primary: Writer, val intercept: (String) -> Unit = {}) : Writer() {

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        primary.write(cbuf, off, len)
        intercept(String(cbuf, off, len))
    }

    override fun write(str: String, off: Int, len: Int) {
        primary.write(str, off, len)
        intercept(str)
    }

    override fun flush() {
        primary.flush()
    }

    override fun close() {
        primary.close()
    }
}