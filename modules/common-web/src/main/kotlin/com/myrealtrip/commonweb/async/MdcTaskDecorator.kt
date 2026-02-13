package com.myrealtrip.commonweb.async

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator

/**
 * TaskDecorator that propagates MDC context to async threads.
 *
 * Usage:
 * ```kotlin
 * @Bean
 * fun taskExecutor(): ThreadPoolTaskExecutor {
 *     return ThreadPoolTaskExecutor().apply {
 *         setTaskDecorator(MdcTaskDecorator())
 *     }
 * }
 * ```
 */
class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val callerContext = MDC.getCopyOfContextMap()

        return Runnable {
            val previous = MDC.getCopyOfContextMap()
            try {
                setOrClear(callerContext)
                runnable.run()
            } finally {
                setOrClear(previous)
            }
        }
    }

    private fun setOrClear(context: Map<String, String>?) {
        context?.let { MDC.setContextMap(it) } ?: MDC.clear()
    }
}