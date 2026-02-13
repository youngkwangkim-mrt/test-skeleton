package com.myrealtrip.testsupport

import org.junit.jupiter.api.ClassDescriptor
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.ClassOrdererContext

/**
 * Custom JUnit test class execution order.
 *
 * Priority: Embed (1) -> Local (2) -> Test (3) -> Others (4)
 */
class CustomTestClassOrder : ClassOrderer {

    override fun orderClasses(context: ClassOrdererContext) {
        context.classDescriptors.sortWith(compareBy { getOrder(it) })
    }

    private fun getOrder(descriptor: ClassDescriptor): Int =
        when (descriptor.testClass.superclass) {
            IntegratedEmbedTestSupport::class.java -> 1
            IntegratedLocalTestSupport::class.java -> 2
            IntegratedTestSupport::class.java -> 3
            else -> 4
        }
}
