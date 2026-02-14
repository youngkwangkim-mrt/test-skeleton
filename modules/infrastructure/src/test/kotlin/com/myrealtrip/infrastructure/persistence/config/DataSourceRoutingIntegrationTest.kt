package com.myrealtrip.infrastructure.persistence.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

/**
 * @Transactional 프록시를 통한 실제 Master/Slave 라우팅 통합 테스트.
 */
@SpringJUnitConfig(DataSourceRoutingIntegrationTest.TestConfig::class)
class DataSourceRoutingIntegrationTest {

    @Autowired
    private lateinit var routingVerifier: RoutingVerifier

    @Test
    fun `should route to master in write transaction`(): Unit {
        // when
        val readOnly = routingVerifier.executeInWriteTransaction()

        // then
        assertThat(readOnly).isFalse()
    }

    @Test
    fun `should route to slave in read-only transaction`(): Unit {
        // when
        val readOnly = routingVerifier.executeInReadOnlyTransaction()

        // then
        assertThat(readOnly).isTrue()
    }

    @Configuration
    @EnableTransactionManagement
    open class TestConfig {

        @Bean
        open fun masterDataSource(): DataSource =
            DriverManagerDataSource("jdbc:h2:mem:master;DB_CLOSE_DELAY=-1", "sa", "")

        @Bean
        open fun slaveDataSource(): DataSource =
            DriverManagerDataSource("jdbc:h2:mem:slave;DB_CLOSE_DELAY=-1", "sa", "")

        @Bean
        open fun routingDataSource(
            masterDataSource: DataSource,
            slaveDataSource: DataSource,
        ): DataSource {
            val routing = object : AbstractRoutingDataSource() {
                override fun determineCurrentLookupKey(): Any =
                    if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) "slave" else "master"
            }
            routing.setTargetDataSources(
                mapOf<Any, Any>("master" to masterDataSource, "slave" to slaveDataSource),
            )
            routing.setDefaultTargetDataSource(masterDataSource)
            return routing
        }

        @Bean
        open fun dataSource(routingDataSource: DataSource): DataSource =
            LazyConnectionDataSourceProxy(routingDataSource)

        @Bean
        open fun transactionManager(dataSource: DataSource): DataSourceTransactionManager =
            DataSourceTransactionManager(dataSource)

        @Bean
        open fun routingVerifier(): RoutingVerifier = RoutingVerifier()
    }

    open class RoutingVerifier {

        @Transactional
        open fun executeInWriteTransaction(): Boolean =
            TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        @Transactional(readOnly = true)
        open fun executeInReadOnlyTransaction(): Boolean =
            TransactionSynchronizationManager.isCurrentTransactionReadOnly()
    }
}
