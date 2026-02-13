package com.myrealtrip.common.codes

/**
 * Common code interface
 */
interface CommonCode {

    /**
     * code
     */
    val code: String

    /**
     * description
     */
    val description: String

    /**
     * response code name (Enum)
     */
    val name: String

}