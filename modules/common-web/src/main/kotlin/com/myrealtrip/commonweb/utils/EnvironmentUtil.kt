package com.myrealtrip.commonweb.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class EnvironmentUtil(
    private val environment: Environment,
    //
    @param:Value("\${spring.profiles.active:}")
    private val activeProfile: String = "",
    //
    @param:Value("\${spring.application.name:test}")
    private val name: String = "",
    //
    @param:Value("\${spring.application.version:0.0.0}")
    private val version: String = ""
) {

    companion object {
        const val LOCAL_PROFILE_PREFIX = "local"
        const val DEV_PROFILE_PREFIX = "dev"
        const val TEST_PROFILE_PREFIX = "test"
        const val STAGE_PROFILE_PREFIX = "stage"
        const val PROD_PROFILE_PREFIX = "prod"
    }

    private val isProductionLazy: Boolean by lazy {
        environment.activeProfiles.any { it.startsWith(PROD_PROFILE_PREFIX, ignoreCase = true) }
    }

    private val isStagingLazy: Boolean by lazy {
        environment.activeProfiles.any { it.startsWith(STAGE_PROFILE_PREFIX, ignoreCase = true) }
    }

    private val isTestLazy: Boolean by lazy {
        environment.activeProfiles.any { it.startsWith(TEST_PROFILE_PREFIX, ignoreCase = true) }
    }

    private val isDevelopmentLazy: Boolean by lazy {
        environment.activeProfiles.any { it.startsWith(DEV_PROFILE_PREFIX, ignoreCase = true) }
    }

    private val isLocalLazy: Boolean by lazy {
        environment.activeProfiles.any { it.startsWith(LOCAL_PROFILE_PREFIX, ignoreCase = true) }
    }

    private val activeProfilesLazy: List<String> by lazy {
        environment.activeProfiles.toList()
    }

    fun appName(): String = name

    fun version(): String = version

    fun activeProfile(): String = activeProfile

    fun activeProfiles(): List<String> = activeProfilesLazy

    fun isProduction(): Boolean = isProductionLazy

    fun isNonProduction(): Boolean = !isProductionLazy

    fun isStagingOrProduction(): Boolean = isStagingLazy || isProductionLazy

    fun isStaging(): Boolean = isStagingLazy

    fun isTest(): Boolean = isTestLazy

    fun isDevelopment(): Boolean = isDevelopmentLazy

    fun isLocal(): Boolean = isLocalLazy

    fun isDevOrTest(): Boolean = isDevelopmentLazy || isTestLazy

}