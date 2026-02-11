package name.pomelo.parent_child_ipc.helpers

import java.io.File

import name.pomelo.parent_child_ipc.helpers.Logging.logInfo
import name.pomelo.parent_child_ipc.helpers.Logging.logErr

object ConfigFileReader {

    fun decode(value: String): String {
        if (value == "'") {
            // Seems appropriate and handles a special case of "single quotes at both ends"
            return "'"
        }
        else if (value.startsWith("'") && value.endsWith("'")) {
            // There seems to be no need for unescaping anything inside the de-quoted string for now
            return value.substring(1, value.length - 1)
        }
        else {
            return value
        }
    }

    // This code suggested by GPT. Looks good.

    fun loadConfigMap(filePath: String): Map<String, String> {
        val configMap = mutableMapOf<String, String>()
        try {
            File(filePath).useLines(Charsets.UTF_8) { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    // Skip empty lines and comments
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = decode(parts[1].trim())
                            configMap[key] = value
                        } else {
                            logInfo("Skipping malformed config line: $trimmed")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logErr("Failed to load config file '$filePath'", ex, "CONFIG")
            error("Cannot continue without configuration")
        }
        return configMap
    }

}