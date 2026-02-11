package name.pomelo.parent_child_ipc.helpers

/*
 * An item that would be found on the command line.
 * Such an item appears as a single "argument" given to main.
 * This may be a conventional "short option" (e.g. "-v") or a "long option" (e.g. "--verbose")
 * It may be something else (e.g. for git, "subcommands" that do not fit in the option or option+value schema, are allowed.
 * If there are values (arguments) that shall be associated with an option:
 *
 * Under the POSIX Utility Syntax Guidelines:
 * ------------------------------------------
 * Short options are introduced with a single - and a single letter (e.g. -o)
 * If an option takes an argument, that argument is a *separate argv element*
 * These are not portable POSIX (even though they’re common):
 * cmd -ovalue   # common GNU extension
 * cmd -o=value  # GNU-style, not POSIX
 * A POSIX-conforming utility does not have to accept those forms.
 * POSIX does allow grouping of short options only when none of them take arguments:
 * cmd -abc      # equivalent to -a -b -c (if a, b, c take no args)
 * If one of the options requires an argument, grouping becomes ambiguous, and POSIX does not define -ovalue as valid.
 *
 * Long options
 * ------------
 * POSIX does not define long options at all.
 * In POSIX:
 * Options are single letters introduced by a single -
 * Anything starting with -- has no special meaning (except a convention, see below)
 * So --long-option is a GNU (and generally non-POSIX) extension.
 * Other ecosystems adopted it later (BSD, BusyBox, etc.), but POSIX itself is silent.
 *
 * POSIX does recognize a convention where "--" means “end of options” — everything after it is treated as
 * operands, even if it starts with -.
 *
 * How GNU long options work
 *
 * In GNU-style utilities (using getopt_long):
 * Both of these are valid and equivalent:
 * --output=file.txt
 * --output file.txt
 *
 * So:
 * = may be used to attach the argument
 * whitespace may also be used
 * This is GNU-defined behavior, not POSIX.
 *
 * Some non-GNU tools accept long options but only one of the two forms (usually space-separated).
 * Since POSIX doesn’t standardize this, you can’t assume consistency outside GNU.
 */

/**
 * We have 1 generic constructor and two specific constructors:
 * 1) A constructor taking a value (i.e. not a keyword-value / option-argument pair, probably a subcommand)
 *    The type will be VALUE, the keyword will be null, and the value will be set to the given value.
 * 2) A constructor taking a keyword and a value (i.e. an option-argument pair)
 *    The type will be LONG_OPTION or SHORT_OPTION depending on whether the keyword
 *    starts with "-" or "--" (i.e. the keyword includes the option-specific dash)
 *    and the value will be set to the given value.
 *
 * The corresponding command line would be:
 * A command line in which the keyword and value are always separated by a blank,
 * corresponding to POSIX and GNU conventions:
 * [-o value] or [--option value]
 * ... but not:
 * [-ovalue] [-o=value] [--option=value]
 */

class CmdLineItem(val type: Type, val keyword: String?, val value: String?) {

    // Note that a "CmdLineItem" is not the result of parsing, it is set up
    // by the programmer. The programmer knows exactly what it is supposed to be!
    // Thus, we can set the "type".

    enum class Type { LONG_OPTION, SHORT_OPTION, VALUE }

    constructor (value: String) : this(Type.VALUE, null, value)
    constructor (keyword: String, value: String?) : this(optionTypeFromKeyword(keyword), keyword, value)

    companion object {
        val dualDashRegex = Regex("""^--\w.*""")
        val singleDashRegex = Regex("""^-\w.*""")

        // Never returns "VALUE", looks at whether the key starts with "-" or "--"

        fun optionTypeFromKeyword(keyword: String): Type {
            return if (dualDashRegex.matches(keyword)) Type.LONG_OPTION
            else if (singleDashRegex.matches(keyword)) Type.SHORT_OPTION
            else error("Keyword '$keyword' does not start with '--' or '-'")
        }

        fun initCheckIfTypeIsValue(keyword: String?, value: String?) {
            // Technically, one COULD have situations where the value is blank, but, really, ...
            require(value != null && value.isNotBlank()) { "Value must be set to something non-blank if type indicates a value." }
            if (keyword != null) {
                System.err.println("Keyword is non-null but the type indicates a value. It will be ignored.")
            }
        }

        fun initCheckIfTypeIsLongOrShortOption(type: Type, keyword: String?) {
            require(keyword != null) { "Keyword must not be null if the type indicates an option." }
            require(keyword.isNotBlank()) { "Keyword must not be blank if the type indicates an option." }
            require(keyword.trim() == keyword) { "Keyword must not be trimmable if the type indicates an option." }
            if (type == Type.LONG_OPTION) {
                require(keyword.matches(dualDashRegex)) { "Keyword must start with '--' if the type indicates a 'long option'." }
            }
            if (type == Type.SHORT_OPTION) {
                require(keyword.matches(singleDashRegex) && !keyword.startsWith("--")) { "Keyword must start with a single '-'  if the type indicates a 'long option'." }
            }
        }
    }

    init {
        if (type == Type.VALUE) {
            initCheckIfTypeIsValue(keyword, value)
        }
        if (type == Type.LONG_OPTION || type == Type.SHORT_OPTION) {
            initCheckIfTypeIsLongOrShortOption(type, keyword)
        }
    }

    fun toStringList(): List<String> {
        return when (type) {
            Type.LONG_OPTION -> {
                // the keyword includes the "--" already
                // If there is a value after the keyword, use the "separation by blank" approach
                if (value == null) {
                    listOf(keyword!!)
                } else {
                    listOf(keyword!!, value)
                }
            }

            Type.SHORT_OPTION -> {
                // the keyword includes the "-" already
                // If there is a value after the keyword, use the "separation by blank" approach
                if (value == null) {
                    listOf(keyword!!)
                } else {
                    listOf(keyword!!, value)
                }
            }

            Type.VALUE -> {
                listOf(value!!)
            }
        }
    }
}