package com.myrealtrip.commonweb.aop.allowedip

import io.github.oshai.kotlinlogging.KotlinLogging
import com.myrealtrip.common.annoatations.CheckIp
import com.myrealtrip.commonweb.utils.IpAddrUtil
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order

private val logger = KotlinLogging.logger {}

/**
 * Aspect that checks if client IP is allowed based on [CheckIp] annotation.
 *
 * IP check order:
 * 1. Local IPs (127.0.0.1, localhost, ::1) - always allowed
 * 2. IPs specified in [CheckIp.allowedIps] - supports exact, wildcard, CIDR patterns
 */
@Aspect
@Order(3)
class CheckIpAspect {

    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    @Pointcut("@within(com.myrealtrip.common.annoatations.CheckIp) || @annotation(com.myrealtrip.common.annoatations.CheckIp)")
    fun checkIpPointcut() {
    }

    @Before("checkIpPointcut()")
    fun checkClientIp(joinPoint: JoinPoint) {
        val annotation = findAnnotation(joinPoint) ?: return
        val clientIp = IpAddrUtil.getClientIp()
        val allowedPatterns = IpWhitelist.LOCAL_IPS + annotation.allowedIps.toList()

        if (!IpWhitelist.isWhitelisted(clientIp, allowedPatterns)) {
            throw UnauthorizedIpException(clientIp)
        }

        logger.debug { "Allowed IP: $clientIp" }
    }

    private fun findAnnotation(joinPoint: JoinPoint): CheckIp? {
        val methodSignature = joinPoint.signature as? MethodSignature
            ?: return null

        // Method-level annotation takes precedence
        return AnnotationUtils.findAnnotation(methodSignature.method, CheckIp::class.java)
            ?: AnnotationUtils.findAnnotation(joinPoint.target.javaClass, CheckIp::class.java)
    }

}