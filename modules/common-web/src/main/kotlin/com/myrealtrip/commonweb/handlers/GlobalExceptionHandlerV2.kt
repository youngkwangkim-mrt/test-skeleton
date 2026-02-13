package com.myrealtrip.commonweb.handlers

import com.myrealtrip.common.codes.ResponseCode
import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.BizException
import com.myrealtrip.common.exceptions.BizRuntimeException
import com.myrealtrip.common.exceptions.KnownException
import com.myrealtrip.commonweb.response.resource.ApiResource
import com.myrealtrip.commonweb.security.TokenErrorCode
import com.myrealtrip.commonweb.utils.ErrorLogPrintUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestValueException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

private val logger = KotlinLogging.logger {}

/**
 * Global exception handler for REST controllers.
 *
 * @see BizRuntimeException
 * @see BizException
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalExceptionHandlerV2 {

    init {
        logger.info { "# ==> ${javaClass.simpleName} initialized" }
    }

    @ExceptionHandler(KnownException::class)
    fun handleKnownException(
        request: HttpServletRequest,
        e: KnownException,
    ): ResponseEntity<ApiResource<Any>> =
        createApiResource(request, e.code, e, log = false)

    @ExceptionHandler(BizRuntimeException::class)
    fun handleBizRuntimeException(
        request: HttpServletRequest,
        e: BizRuntimeException,
    ): ResponseEntity<ApiResource<Any>> =
        createApiResource(request, e.code, e, e.logStackTrace)

    @ExceptionHandler(BizException::class)
    fun handleBizException(
        request: HttpServletRequest,
        e: BizException,
    ): ResponseEntity<ApiResource<Any>> =
        createApiResource(request, e.code, e, e.logStackTrace)

    @ExceptionHandler(Exception::class)
    fun handleException(
        request: HttpServletRequest,
        ex: Exception,
    ): ResponseEntity<ApiResource<Any>> = when (ex) {
        is NoResourceFoundException,
        is HttpRequestMethodNotSupportedException,
            -> createApiResource(request, ErrorCode.NOT_FOUND, ex)

        is MethodArgumentNotValidException -> {
            val errors = ex.bindingResult.allErrors
                .filterIsInstance<FieldError>()
                .associate { it.field to it.defaultMessage }
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, printStackTrace = false, data = errors)
        }

        is MethodArgumentTypeMismatchException -> {
            val detail = "Type mismatch: '${ex.value}' is not acceptable in property '${ex.propertyName}'"
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, printStackTrace = false, data = detail)
        }

        is MissingRequestValueException -> {
            val detail = ex.body.detail ?: ErrorCode.INVALID_ARGUMENT.message
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, printStackTrace = false, data = detail)
        }

        is MissingServletRequestPartException -> {
            val detail = ex.body.detail ?: ErrorCode.INVALID_ARGUMENT.message
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, data = detail)
        }

        is HandlerMethodValidationException -> {
            val errors = ex.parameterValidationResults
                .flatMap { it.resolvableErrors }
                .associate { it.codes?.lastOrNull().orEmpty() to it.defaultMessage }
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, printStackTrace = false, data = errors)
        }

        is HttpMessageNotReadableException,
        is HttpMediaTypeNotSupportedException,
            -> {
            val message = ex.message?.split(":")?.firstOrNull() ?: ErrorCode.NOT_READABLE.message
            createApiResource(request, ErrorCode.NOT_READABLE, ex, printStackTrace = false, data = message)
        }

        is ConstraintViolationException -> {
            val errors = ex.constraintViolations
                .associate { it.propertyPath.toString() to it.message }
            createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex, printStackTrace = false, data = errors)
        }

        is MultipartException -> createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex)

        is AuthenticationException -> createApiResource(request, ErrorCode.UNAUTHORIZED, ex)

        is AccessDeniedException -> createApiResource(request, ErrorCode.FORBIDDEN, ex)

        is JwtException -> createApiResource(request, TokenErrorCode.TOKEN_ERROR, ex)

        is IllegalArgumentException -> createApiResource(request, ErrorCode.ILLEGAL_ARGUMENT, ex)

        is IllegalStateException -> createApiResource(request, ErrorCode.ILLEGAL_STATE, ex)

        is NoSuchElementException -> createApiResource(request, ErrorCode.DATA_NOT_FOUND, ex)

        is UnsupportedOperationException -> createApiResource(request, ErrorCode.UNSUPPORTED_OPERATION, ex)

        else -> createApiResource(request, ErrorCode.SERVER_ERROR, ex, data = ErrorCode.SERVER_ERROR.message)
    }

    companion object {

        @JvmOverloads
        fun createApiResource(
            request: HttpServletRequest,
            code: ResponseCode,
            e: Exception,
            printStackTrace: Boolean = true,
            log: Boolean = true,
            data: Any = e.message ?: code.message,
        ): ResponseEntity<ApiResource<Any>> {
            if (log) {
                ErrorLogPrintUtil.logError(request, code, e, printStackTrace)
            }
            return ApiResource.of(code = code, data = data)
        }
    }
}