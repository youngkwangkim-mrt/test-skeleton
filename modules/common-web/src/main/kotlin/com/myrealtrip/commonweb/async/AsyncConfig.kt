package com.myrealtrip.commonweb.async

import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.VirtualThreadTaskExecutor
import org.springframework.core.task.support.ContextPropagatingTaskDecorator
import org.springframework.core.task.support.TaskExecutorAdapter
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.util.concurrent.Executor

@Configuration
class AsyncConfig(
    private val contextPropagatingTaskDecorator: ContextPropagatingTaskDecorator
) : AsyncConfigurer {

    companion object {
        const val ASYNC_THREAD_PREFIX: String = "async-vt-"
    }

    override fun getAsyncExecutor(): Executor? {
        return asyncVirtualThreadTaskExecutor()
    }

//    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler? {
//        return GlobalSimpleAsyncUncaughtExceptionHandler()
//    }

    private fun asyncVirtualThreadTaskExecutor(): AsyncTaskExecutor {
        val executorAdapter = TaskExecutorAdapter(VirtualThreadTaskExecutor(ASYNC_THREAD_PREFIX))
        executorAdapter.setTaskDecorator(contextPropagatingTaskDecorator)
        return executorAdapter
    }

}