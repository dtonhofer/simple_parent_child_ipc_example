package name.pomelo.parent_child_ipc.configuration

import name.pomelo.parent_child_ipc.ParentMain
import name.pomelo.parent_child_ipc.helpers.ConfigFileReader
import name.pomelo.parent_child_ipc.helpers.Logging
import java.io.File

object ParentConfigFileAnalysis {

    data class ConfigData(
        val javaExe: String,
        val jarFile: String,
        val workDir: String,
        val withParentAccidents: Boolean,
        val withChildAccidents: Boolean,
        val argsBeyondDashDash: List<String>
    )

    data class KeyAndDescription(val key: String?, val description: String) {
        override fun toString(): String {
            return if (key == null) {
                description
            } else {
                "$key ($description)"
            }
        }
    }

    private enum class NeedWhat { NEED_FILE, NEED_DIR }

    private fun configFileValueNotFound(kad: KeyAndDescription, configFilePath: String): Nothing {
        error("A value for '$kad' was not found in config file '$configFilePath'.")
    }

    private fun isNeedsHomeInterpolation(path: String): Boolean {
        return (path.startsWith("~/") || path.startsWith($$"$HOME/") || path == $$"$HOME")
    }

    private fun performHomeInterpolation(kad: KeyAndDescription, filePathRaw: String): String {
        return if (isNeedsHomeInterpolation(filePathRaw)) {
            val userHome = findHomeDir()
            if (userHome == null) {
                error("Cannot find 'home directory' but the value for '$kad' is '$filePathRaw' and I need it.")
            } else if (!File(userHome).isDirectory) {
                error("Obtained a home directory needed for '$kad', namely '$userHome', but that is not a directory.")
            } else {
                // according to needsHomeInterpolation(), one of these is true:
                if (filePathRaw.startsWith("~/")) {
                    filePathRaw.replaceFirst("~", userHome)
                } else if (filePathRaw.startsWith($$"$HOME")) {
                    filePathRaw.replaceFirst($$"$HOME", userHome)
                } else {
                    error("This should not happen.")
                }
            }
        } else {
            filePathRaw
        }
    }

    private fun findFilePathInConfig(kad: KeyAndDescription, needWhat: NeedWhat, configMap: Map<String, String>, configFilePath: String): String {
        val filePathRaw = configMap[kad.key] ?: configFileValueNotFound(kad, configFilePath)
        val filePathCooked: String = performHomeInterpolation(kad, filePathRaw)
        val text = "The value picked up from the config file '$configFilePath' under key '${kad.key}' ('${kad.description}') resolves to '$filePathCooked' but"
        if (!File(filePathCooked).exists()) {
            error("$text this filesystem entry does not exist.")
        } else if (needWhat == NeedWhat.NEED_FILE && !File(filePathCooked).isFile) {
            error("$text this filesystem entry is not a file.")
        } else if (needWhat == NeedWhat.NEED_DIR && !File(filePathCooked).isDirectory) {
            error("$text this filesystem entry is not a directory.")
        }
        return filePathCooked
    }

    private fun findJarFileInConfig(configMap: Map<String, String>, configFilePath: String): String {
        return findFilePathInConfig(KeyAndDescription("childJarFile", "the jar file to run the child process"), NeedWhat.NEED_FILE, configMap, configFilePath)
    }

    private fun findJavaExeInConfig(configMap: Map<String, String>, configFilePath: String): String {
        return findFilePathInConfig(KeyAndDescription("javaExe", "the java executable"), NeedWhat.NEED_FILE, configMap, configFilePath)
    }

    private fun findWorkDirInConfig(configMap: Map<String, String>, configFilePath: String): String {
        return findFilePathInConfig(KeyAndDescription("workDir", "the working directory for the child process"), NeedWhat.NEED_DIR, configMap, configFilePath)
    }

    private fun findHomeDir(): String? {
        return System.getProperty("user.home")
    }

    private fun isConfigFilePathGood(configFilePath: String): Boolean {
        if (!File(configFilePath).exists()) {
            Logging.logErr("Config file '$configFilePath' does not exist", ParentMain.parentMarker)
            return false
        }
        if (!File(configFilePath).isFile) {
            Logging.logErr("Config file '$configFilePath' is not a regular file.", ParentMain.parentMarker)
            return false
        }
        if (!File(configFilePath).canRead()) {
            Logging.logErr("Config file '$configFilePath' cannot be read.", ParentMain.parentMarker)
            return false
        }
        return true
    }

    // TODO: The code tree unerneath this point is a disagreeable mix of "return
    // TODO: values signifying failures" and "throw an error signifying failure".
    // TODO: Should be uniformized to "return values signifying failures" (or not?)

    fun analyzeArgvAndFindConfigData(argv: List<String>): Pair<Boolean, ConfigData?> {
        val (itWorked, argvAnalysisResult) = ParentArgvAnalysis.analyzeArgv(argv)
        if (itWorked) {
            assert(argvAnalysisResult != null)
            val configFilePathRaw = argvAnalysisResult!!.configFilePath
            val configFilePathCooked = performHomeInterpolation(KeyAndDescription(null, "the path to the config file given on the command line"), configFilePathRaw)

            if (isConfigFilePathGood(configFilePathCooked)) {
                val configMap = ConfigFileReader.loadConfigMap(configFilePathCooked)
                val jarFile = findJarFileInConfig(configMap, configFilePathCooked)
                val javaExe = findJavaExeInConfig(configMap, configFilePathCooked)
                val workDir = findWorkDirInConfig(configMap, configFilePathCooked)
                // Success
                return Pair(
                    true, ConfigData(
                        javaExe,
                        jarFile,
                        workDir,
                        argvAnalysisResult.withParentAccidents,
                        argvAnalysisResult.withChildAccidents,
                        argvAnalysisResult.argsBeyondDashDash
                    )
                )
            }
        }
        // Failure
        return Pair(false, null)
    }
}