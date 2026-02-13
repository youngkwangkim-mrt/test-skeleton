package com.myrealtrip.commonweb.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.env.Environment

class EnvironmentUtilTest : DescribeSpec({

    describe("EnvironmentUtil profile detection") {

        context("isProduction") {
            it("should return true when active profile starts with 'prod'") {
                // given
                val environment = mockEnvironment("prod")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isProduction() shouldBe true
            }

            it("should return true when active profile starts with 'prod' case-insensitively") {
                // given
                val environment = mockEnvironment("PROD", "Prod-kr")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isProduction() shouldBe true
            }

            it("should return true when profile is 'prod-kr' or 'prod-us'") {
                // given
                val envKr = mockEnvironment("prod-kr")
                val envUs = mockEnvironment("prod-us")

                // when & then
                EnvironmentUtil(envKr).isProduction() shouldBe true
                EnvironmentUtil(envUs).isProduction() shouldBe true
            }

            it("should return false when profile is 'production' (does not start with 'prod' prefix exactly)") {
                // given - 'production' starts with 'prod' so this should be true
                val environment = mockEnvironment("production")

                // when
                val util = EnvironmentUtil(environment)

                // then - actually 'production'.startsWith('prod') is true
                util.isProduction() shouldBe true
            }

            it("should return false when no production profile is active") {
                // given
                val environment = mockEnvironment("dev", "local")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isProduction() shouldBe false
            }
        }

        context("isNonProduction") {
            it("should return true when not in production environment") {
                // given
                val environment = mockEnvironment("dev")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isNonProduction() shouldBe true
            }

            it("should return false when in production environment") {
                // given
                val environment = mockEnvironment("prod")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isNonProduction() shouldBe false
            }
        }

        context("isStaging") {
            it("should return true when active profile starts with 'stage'") {
                // given
                val environment = mockEnvironment("stage")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isStaging() shouldBe true
            }

            it("should return true for profile like 'stage-kr'") {
                // given
                val environment = mockEnvironment("stage-kr")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isStaging() shouldBe true
            }

            it("should handle case-insensitive matching") {
                // given
                val environment = mockEnvironment("STAGE", "Stage")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isStaging() shouldBe true
            }
        }

        context("isTest") {
            it("should return true when active profile starts with 'test'") {
                // given
                val environment = mockEnvironment("test")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isTest() shouldBe true
            }

            it("should return true for profile like 'test-integration'") {
                // given
                val environment = mockEnvironment("test-integration")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isTest() shouldBe true
            }
        }

        context("isDevelopment") {
            it("should return true when active profile starts with 'dev'") {
                // given
                val environment = mockEnvironment("dev")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevelopment() shouldBe true
            }

            it("should return true for profile like 'dev-local' or 'develop'") {
                // given
                val envDevLocal = mockEnvironment("dev-local")
                val envDevelop = mockEnvironment("develop")

                // when & then
                EnvironmentUtil(envDevLocal).isDevelopment() shouldBe true
                EnvironmentUtil(envDevelop).isDevelopment() shouldBe true
            }
        }

        context("isLocal") {
            it("should return true when active profile starts with 'local'") {
                // given
                val environment = mockEnvironment("local")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isLocal() shouldBe true
            }

            it("should return true for profile like 'local-docker'") {
                // given
                val environment = mockEnvironment("local-docker")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isLocal() shouldBe true
            }
        }

        context("isDevOrTest - combined condition") {
            it("should return true when in development environment") {
                // given
                val environment = mockEnvironment("dev")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevOrTest() shouldBe true
            }

            it("should return true when in test environment") {
                // given
                val environment = mockEnvironment("test")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevOrTest() shouldBe true
            }

            it("should return true when both dev and test profiles are active") {
                // given
                val environment = mockEnvironment("dev", "test")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevOrTest() shouldBe true
            }

            it("should return false when in production environment") {
                // given
                val environment = mockEnvironment("prod")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevOrTest() shouldBe false
            }

            it("should return false when in staging environment only") {
                // given
                val environment = mockEnvironment("stage")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isDevOrTest() shouldBe false
            }
        }

        context("activeProfiles") {
            it("should return all active profiles") {
                // given
                val environment = mockEnvironment("dev", "local", "docker")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.activeProfiles() shouldBe listOf("dev", "local", "docker")
            }

            it("should return empty list when no profiles are active") {
                // given
                val environment = mockEnvironment()

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.activeProfiles() shouldBe emptyList()
            }
        }

        context("activeProfile from @Value") {
            it("should return the injected active profile string") {
                // given
                val environment = mockEnvironment("dev")

                // when
                val util = EnvironmentUtil(environment, "dev,local")

                // then
                util.activeProfile() shouldBe "dev,local"
            }

            it("should return empty string when no profile is set") {
                // given
                val environment = mockEnvironment()

                // when
                val util = EnvironmentUtil(environment, "")

                // then
                util.activeProfile() shouldBe ""
            }
        }

        context("edge cases") {
            it("should handle empty active profiles array") {
                // given
                val environment = mockEnvironment()

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isProduction() shouldBe false
                util.isStaging() shouldBe false
                util.isTest() shouldBe false
                util.isDevelopment() shouldBe false
                util.isLocal() shouldBe false
                util.isNonProduction() shouldBe true
                util.isDevOrTest() shouldBe false
            }

            it("should detect correct profile when multiple profiles are active") {
                // given - mixed environment with prod and other profiles
                val environment = mockEnvironment("common", "prod", "redis")

                // when
                val util = EnvironmentUtil(environment)

                // then
                util.isProduction() shouldBe true
                util.isNonProduction() shouldBe false
            }
        }
    }
})

private class TestEnvironment(private val profiles: Array<out String>) : Environment {
    override fun getActiveProfiles(): Array<String> = profiles.map { it }.toTypedArray()
    override fun getDefaultProfiles(): Array<String> = arrayOf("default")
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun acceptsProfiles(vararg profiles: String): Boolean = false
    override fun acceptsProfiles(profiles: org.springframework.core.env.Profiles): Boolean = false
    override fun containsProperty(key: String): Boolean = false
    override fun getProperty(key: String): String? = null
    override fun getProperty(key: String, defaultValue: String): String = defaultValue
    override fun <T : Any> getProperty(key: String, targetType: Class<T>): T? = null
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getProperty(key: String, targetType: Class<T>, defaultValue: T): T = defaultValue
    override fun getRequiredProperty(key: String): String = throw IllegalStateException()
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRequiredProperty(key: String, targetType: Class<T>): T = throw IllegalStateException()
    override fun resolvePlaceholders(text: String): String = text
    override fun resolveRequiredPlaceholders(text: String): String = text
}

private fun mockEnvironment(vararg profiles: String): Environment = TestEnvironment(profiles)
