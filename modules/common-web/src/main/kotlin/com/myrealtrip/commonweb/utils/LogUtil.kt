package com.myrealtrip.commonweb.utils

/**
 * Log utility class
 */
object LogUtil {
    const val LOG_LINE =
        "# ================================================================================================="

    private const val SUB_LINE = "# ==================== "
    private const val START = "# ============================== "
    private const val END = " =============================="
    private const val TITLE_MAX_LEN = 35
    private const val TITLE_FORMAT_PATTERN = "$START%-${TITLE_MAX_LEN}s$END"

    /**
     * Log title
     *
     * @param value Title to be logged
     * @return Formatted title string
     */
    @JvmStatic
    fun title(value: String): String {
        val truncatedValue = if (value.length <= TITLE_MAX_LEN) value else value.substring(0, TITLE_MAX_LEN)
        return String.format(TITLE_FORMAT_PATTERN, truncatedValue)
    }

    /**
     * Log subtitle
     *
     * @param value Subtitle to be logged
     * @return Formatted subtitle string
     */
    @JvmStatic
    fun subtitle(value: String): String {
        return SUB_LINE + value
    }

}
