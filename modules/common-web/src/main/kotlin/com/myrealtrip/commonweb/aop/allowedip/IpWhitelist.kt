package com.myrealtrip.commonweb.aop.allowedip

/**
 * IP whitelist checker supporting exact match, wildcard patterns, and CIDR notation.
 *
 * ## Supported Formats
 * - **Exact**: `192.168.1.100`
 * - **Wildcard**: `192.168.*.*`, `*`
 * - **CIDR**: `192.168.0.0/24`, `10.0.0.0/8`
 *
 * ## Usage
 * ```kotlin
 * // Check if IP is local/loopback
 * IpWhitelist.isWhitelisted("127.0.0.1")  // true
 *
 * // Check against custom patterns
 * IpWhitelist.isWhitelisted("192.168.1.100", listOf("192.168.0.0/16"))  // true
 *
 * // Check single pattern match
 * IpWhitelist.matches("192.168.1.100", "192.168.*.*")  // true
 * ```
 */
object IpWhitelist {

    private const val WILDCARD = "*"
    private const val CIDR_SEPARATOR = "/"
    private const val IPV4_OCTET_COUNT = 4
    private const val IPV4_BIT_LENGTH = 32

    /** Local/loopback IP addresses (IPv4 and IPv6). */
    val LOCAL_IPS: List<String> = listOf("127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1")

    /**
     * Checks if the IP matches any of the given patterns.
     *
     * @param ip the IP address to check
     * @param patterns list of patterns to match against (default: [LOCAL_IPS])
     * @return true if IP matches any pattern
     */
    fun isWhitelisted(ip: String, patterns: List<String> = LOCAL_IPS): Boolean =
        patterns.any { pattern -> matches(ip, pattern) }

    /**
     * Checks if the IP matches the given pattern.
     *
     * Supports:
     * - Exact match: `192.168.1.100`
     * - Global wildcard: `*`
     * - Wildcard octets: `192.168.*.*`
     * - CIDR notation: `192.168.0.0/24`
     *
     * @param ip the IP address to check
     * @param pattern the pattern to match against
     * @return true if IP matches the pattern
     */
    fun matches(ip: String, pattern: String): Boolean {
        if (pattern == WILDCARD) return true

        // CIDR notation (e.g., 192.168.0.0/24)
        if (CIDR_SEPARATOR in pattern) {
            return matchesCidr(ip, pattern)
        }

        // Exact match (no wildcard)
        if (WILDCARD !in pattern) return ip == pattern

        // Wildcard pattern (e.g., 192.168.*.*)
        return matchesWildcard(ip, pattern)
    }

    private fun matchesWildcard(ip: String, pattern: String): Boolean {
        val ipOctets = ip.split(".")
        val patternOctets = pattern.split(".")

        if (ipOctets.size != IPV4_OCTET_COUNT || patternOctets.size != IPV4_OCTET_COUNT) {
            return false
        }

        return ipOctets.zip(patternOctets).all { (ipOctet, patternOctet) ->
            patternOctet == WILDCARD || ipOctet == patternOctet
        }
    }

    private fun matchesCidr(ip: String, cidr: String): Boolean {
        val parts = cidr.split(CIDR_SEPARATOR)
        if (parts.size != 2) return false

        val baseIp = parts[0]
        val prefixLength = parts[1].toIntOrNull() ?: return false

        if (prefixLength !in 0..IPV4_BIT_LENGTH) return false

        val ipInt = ipToInt(ip) ?: return false
        val baseIpInt = ipToInt(baseIp) ?: return false
        val mask = createSubnetMask(prefixLength)

        return (ipInt and mask) == (baseIpInt and mask)
    }

    private fun ipToInt(ip: String): Int? {
        val octets = ip.split(".")
        if (octets.size != IPV4_OCTET_COUNT) return null

        return try {
            octets.fold(0) { acc, octet ->
                val value = octet.toInt()
                if (value !in 0..255) return null
                (acc shl 8) or value
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun createSubnetMask(prefixLength: Int): Int {
        if (prefixLength == 0) return 0
        return -1 shl (IPV4_BIT_LENGTH - prefixLength)
    }
}