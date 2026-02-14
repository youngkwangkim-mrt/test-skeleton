package com.myrealtrip.infrastructure.persistence.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.sql.Connection
import javax.sql.DataSource

class DataSourceConfigTest {

    private val config = DataSourceConfig()

    @Nested
    inner class ConditionalOnPropertyTest {

        private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceConfig::class.java))

        @Test
        fun `should not register beans when master jdbc-url property is absent`(): Unit {
            // given & when & then
            contextRunner.run { context ->
                assertThat(context).doesNotHaveBean(DataSourceConfig::class.java)
                assertThat(context).doesNotHaveBean("masterDataSource")
                assertThat(context).doesNotHaveBean("slaveDataSource")
                assertThat(context).doesNotHaveBean("routingDataSource")
            }
        }

        @Test
        fun `should register beans when master jdbc-url property is present`(): Unit {
            // given & when & then
            contextRunner
                .withPropertyValues(
                    "spring.datasource.master.hikari.jdbc-url=jdbc:h2:mem:master",
                    "spring.datasource.slave.hikari.jdbc-url=jdbc:h2:mem:slave",
                )
                .run { context ->
                    assertThat(context).hasSingleBean(DataSourceConfig::class.java)
                    assertThat(context).hasBean("masterDataSource")
                    assertThat(context).hasBean("slaveDataSource")
                    assertThat(context).hasBean("routingDataSource")
                    assertThat(context).hasBean("dataSource")
                }
        }
    }

    private val masterConnection: Connection = mock()
    private val slaveConnection: Connection = mock()
    private val masterDataSource: DataSource = mock<DataSource>().also {
        whenever(it.connection).thenReturn(masterConnection)
    }
    private val slaveDataSource: DataSource = mock<DataSource>().also {
        whenever(it.connection).thenReturn(slaveConnection)
    }

    @AfterEach
    fun tearDown(): Unit {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
            TransactionSynchronizationManager.clear()
        }
    }

    @Nested
    inner class RoutingDataSourceTest {

        private val routingDataSource = config.routingDataSource(masterDataSource, slaveDataSource)
            .also { (it as AbstractRoutingDataSource).afterPropertiesSet() }

        @Test
        fun `should route to master when transaction is not read-only`(): Unit {
            // given
            TransactionSynchronizationManager.initSynchronization()
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)

            // when
            val connection = routingDataSource.connection

            // then
            assertThat(connection).isSameAs(masterConnection)
        }

        @Test
        fun `should route to slave when transaction is read-only`(): Unit {
            // given
            TransactionSynchronizationManager.initSynchronization()
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)

            // when
            val connection = routingDataSource.connection

            // then
            assertThat(connection).isSameAs(slaveConnection)
        }

        @Test
        fun `should default to master when no transaction context`(): Unit {
            // given - no transaction synchronization

            // when
            val connection = routingDataSource.connection

            // then
            assertThat(connection).isSameAs(masterConnection)
        }
    }

    @Nested
    inner class PrimaryDataSourceTest {

        @Test
        fun `should wrap routing datasource with LazyConnectionDataSourceProxy`(): Unit {
            // given
            val routingDataSource = config.routingDataSource(masterDataSource, slaveDataSource)
                .also { (it as AbstractRoutingDataSource).afterPropertiesSet() }

            // when
            val primaryDataSource = config.dataSource(routingDataSource)

            // then
            assertThat(primaryDataSource).isInstanceOf(LazyConnectionDataSourceProxy::class.java)
        }
    }
}
