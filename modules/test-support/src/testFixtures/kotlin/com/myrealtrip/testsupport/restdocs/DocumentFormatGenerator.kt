package com.myrealtrip.testsupport.restdocs

import org.springframework.restdocs.snippet.Attributes

/**
 * Utility for generating format attributes in REST Docs field descriptors.
 *
 * For DSL-based usage, prefer using the `formattedAs` infix function or `DATE`/`DATETIME` types
 * which automatically apply format attributes.
 */
object DocumentFormatGenerator {

    fun getDateFormat(format: String = RestDocsAttributes.DATE_FORMAT): Attributes.Attribute =
        RestDocsAttributes.attribute(RestDocsAttributes.KEY_FORMAT, format)

    fun getDateTimeFormat(format: String = RestDocsAttributes.DATETIME_FORMAT): Attributes.Attribute =
        RestDocsAttributes.attribute(RestDocsAttributes.KEY_FORMAT, format)

    fun getDateTimeMsFormat(): Attributes.Attribute =
        RestDocsAttributes.attribute(RestDocsAttributes.KEY_FORMAT, RestDocsAttributes.DATETIME_MS_FORMAT)
}
