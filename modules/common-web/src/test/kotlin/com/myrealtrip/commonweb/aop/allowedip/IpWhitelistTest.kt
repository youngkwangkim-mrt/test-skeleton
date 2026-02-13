package com.myrealtrip.commonweb.aop.allowedip

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class IpWhitelistTest : DescribeSpec({

    describe("matches") {

        describe("exact match") {

            it("should return true when IP exactly matches pattern") {
                IpWhitelist.matches("192.168.1.100", "192.168.1.100") shouldBe true
            }

            it("should return false when IP differs by one octet") {
                IpWhitelist.matches("192.168.1.100", "192.168.1.101") shouldBe false
            }

            it("should match localhost string exactly") {
                IpWhitelist.matches("localhost", "localhost") shouldBe true
            }
        }

        describe("global wildcard (*)") {

            it("should match any valid IPv4 address") {
                IpWhitelist.matches("192.168.1.100", "*") shouldBe true
                IpWhitelist.matches("10.0.0.1", "*") shouldBe true
                IpWhitelist.matches("255.255.255.255", "*") shouldBe true
            }

            it("should match non-IP strings") {
                IpWhitelist.matches("localhost", "*") shouldBe true
            }
        }

        describe("wildcard pattern") {

            it("should match when last octet is wildcard") {
                val pattern = "192.168.1.*"
                IpWhitelist.matches("192.168.1.0", pattern) shouldBe true
                IpWhitelist.matches("192.168.1.255", pattern) shouldBe true
                IpWhitelist.matches("192.168.1.100", pattern) shouldBe true
            }

            it("should not match when fixed octets differ") {
                IpWhitelist.matches("192.168.2.100", "192.168.1.*") shouldBe false
            }

            it("should match with multiple wildcards") {
                val pattern = "192.168.*.*"
                IpWhitelist.matches("192.168.0.0", pattern) shouldBe true
                IpWhitelist.matches("192.168.255.255", pattern) shouldBe true
            }

            it("should match wildcard at first octet") {
                val pattern = "*.168.1.100"
                IpWhitelist.matches("10.168.1.100", pattern) shouldBe true
                IpWhitelist.matches("172.168.1.100", pattern) shouldBe true
            }

            it("should return false when octet count is wrong") {
                IpWhitelist.matches("192.168.1.100", "192.168.*") shouldBe false
                IpWhitelist.matches("192.168.1", "192.168.1.*") shouldBe false
            }
        }

        describe("CIDR notation") {

            describe("/32 - single address") {

                it("should match only exact address") {
                    val cidr = "192.168.1.100/32"
                    IpWhitelist.matches("192.168.1.100", cidr) shouldBe true
                    IpWhitelist.matches("192.168.1.99", cidr) shouldBe false
                    IpWhitelist.matches("192.168.1.101", cidr) shouldBe false
                }
            }

            describe("/24 - 256 addresses") {

                it("should match addresses in range boundary") {
                    val cidr = "192.168.1.0/24"

                    // boundary: first and last
                    IpWhitelist.matches("192.168.1.0", cidr) shouldBe true
                    IpWhitelist.matches("192.168.1.255", cidr) shouldBe true

                    // inside range
                    IpWhitelist.matches("192.168.1.128", cidr) shouldBe true
                }

                it("should not match addresses outside range boundary") {
                    val cidr = "192.168.1.0/24"

                    // just outside boundary
                    IpWhitelist.matches("192.168.0.255", cidr) shouldBe false
                    IpWhitelist.matches("192.168.2.0", cidr) shouldBe false
                }
            }

            describe("/16 - Class B equivalent") {

                it("should match entire second octet range") {
                    val cidr = "192.168.0.0/16"
                    IpWhitelist.matches("192.168.0.0", cidr) shouldBe true
                    IpWhitelist.matches("192.168.255.255", cidr) shouldBe true
                    IpWhitelist.matches("192.169.0.0", cidr) shouldBe false
                }
            }

            describe("/8 - Class A equivalent") {

                it("should match entire first octet range") {
                    val cidr = "10.0.0.0/8"
                    IpWhitelist.matches("10.0.0.0", cidr) shouldBe true
                    IpWhitelist.matches("10.255.255.255", cidr) shouldBe true
                    IpWhitelist.matches("11.0.0.0", cidr) shouldBe false
                }
            }

            describe("/0 - all addresses") {

                it("should match any IPv4 address") {
                    val cidr = "0.0.0.0/0"
                    IpWhitelist.matches("0.0.0.0", cidr) shouldBe true
                    IpWhitelist.matches("255.255.255.255", cidr) shouldBe true
                    IpWhitelist.matches("192.168.1.100", cidr) shouldBe true
                }
            }

            describe("/12 - private network boundary") {

                it("should correctly match 172.16.0.0/12 range") {
                    val cidr = "172.16.0.0/12"

                    // boundary start
                    IpWhitelist.matches("172.16.0.0", cidr) shouldBe true
                    // boundary end
                    IpWhitelist.matches("172.31.255.255", cidr) shouldBe true
                    // just outside
                    IpWhitelist.matches("172.32.0.0", cidr) shouldBe false
                    IpWhitelist.matches("172.15.255.255", cidr) shouldBe false
                }
            }
        }

        describe("invalid input handling") {

            it("should return false for malformed IP address") {
                IpWhitelist.matches("invalid", "192.168.1.0/24") shouldBe false
                IpWhitelist.matches("192.168.1", "192.168.1.0/24") shouldBe false
                IpWhitelist.matches("192.168.1.1.1", "192.168.1.0/24") shouldBe false
            }

            it("should return false for octet value out of range") {
                IpWhitelist.matches("192.168.1.256", "192.168.1.0/24") shouldBe false
                IpWhitelist.matches("192.168.1.-1", "192.168.1.0/24") shouldBe false
            }

            it("should return false for invalid CIDR prefix length") {
                IpWhitelist.matches("192.168.1.100", "192.168.1.0/33") shouldBe false
                IpWhitelist.matches("192.168.1.100", "192.168.1.0/-1") shouldBe false
                IpWhitelist.matches("192.168.1.100", "192.168.1.0/abc") shouldBe false
            }

            it("should return false for invalid CIDR base IP") {
                IpWhitelist.matches("192.168.1.100", "invalid/24") shouldBe false
            }
        }
    }

    describe("isWhitelisted") {

        describe("with custom patterns") {

            it("should return true when IP matches any pattern") {
                val patterns = listOf("10.0.0.0/8", "192.168.0.0/16")
                IpWhitelist.isWhitelisted("10.1.2.3", patterns) shouldBe true
                IpWhitelist.isWhitelisted("192.168.100.1", patterns) shouldBe true
            }

            it("should return false when IP matches no pattern") {
                val patterns = listOf("10.0.0.0/8", "192.168.0.0/16")
                IpWhitelist.isWhitelisted("172.16.1.1", patterns) shouldBe false
            }

            it("should handle empty pattern list") {
                IpWhitelist.isWhitelisted("192.168.1.1", emptyList()) shouldBe false
            }
        }

        describe("with default LOCAL_IPS") {

            it("should allow standard loopback addresses") {
                IpWhitelist.isWhitelisted("127.0.0.1") shouldBe true
                IpWhitelist.isWhitelisted("localhost") shouldBe true
                IpWhitelist.isWhitelisted("::1") shouldBe true
                IpWhitelist.isWhitelisted("0:0:0:0:0:0:0:1") shouldBe true
            }

            it("should reject non-local addresses") {
                IpWhitelist.isWhitelisted("192.168.1.100") shouldBe false
                IpWhitelist.isWhitelisted("10.0.0.1") shouldBe false
            }
        }
    }

    describe("LOCAL_IPS") {

        it("should contain IPv4 and IPv6 loopback addresses") {
            IpWhitelist.LOCAL_IPS shouldBe listOf(
                "127.0.0.1",
                "localhost",
                "0:0:0:0:0:0:0:1",
                "::1",
            )
        }
    }
})
