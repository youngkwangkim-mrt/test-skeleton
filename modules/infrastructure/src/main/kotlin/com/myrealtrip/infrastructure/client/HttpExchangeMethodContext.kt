package com.myrealtrip.infrastructure.client

object HttpExchangeMethodContext {

    private val methodName = ThreadLocal<String>()

    fun set(name: String) = methodName.set(name)

    fun get(): String? = methodName.get()

    fun clear() = methodName.remove()
}
