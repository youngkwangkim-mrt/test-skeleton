package com.myrealtrip.infrastructure.persistence.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

/**
 * Master-Slave DataSource 라우팅 설정.
 *
 * 트랜잭션의 readOnly 속성에 따라 커넥션을 분기한다.
 * - `@Transactional(readOnly = true)` → Slave(Reader)
 * - `@Transactional` (default) → Master(Writer)
 *
 * [LazyConnectionDataSourceProxy]가 실제 SQL 실행 시점까지 커넥션 획득을 지연하므로,
 * Spring이 트랜잭션의 readOnly 플래그를 설정한 뒤에 라우팅이 결정된다.
 *
 * `spring.datasource.master.hikari.jdbc-url` 프로퍼티가 존재할 때만 활성화되므로
 * embed 프로파일에서는 기존 H2 자동 설정이 유지된다.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.master.hikari", name = ["jdbc-url"])
class DataSourceConfig {

    @Bean(MASTER_DATA_SOURCE)
    @ConfigurationProperties("spring.datasource.master.hikari")
    fun masterDataSource(): DataSource =
        DataSourceBuilder.create().build()

    @Bean(SLAVE_DATA_SOURCE)
    @ConfigurationProperties("spring.datasource.slave.hikari")
    fun slaveDataSource(): DataSource =
        DataSourceBuilder.create().build()

    @Bean
    fun routingDataSource(
        @Qualifier(MASTER_DATA_SOURCE) masterDataSource: DataSource,
        @Qualifier(SLAVE_DATA_SOURCE) slaveDataSource: DataSource,
    ): DataSource {
        val routingDataSource = object : AbstractRoutingDataSource() {
            override fun determineCurrentLookupKey(): Any =
                if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) SLAVE else MASTER
        }

        routingDataSource.setTargetDataSources(
            mapOf<Any, Any>(
                MASTER to masterDataSource,
                SLAVE to slaveDataSource,
            ),
        )
        routingDataSource.setDefaultTargetDataSource(masterDataSource)

        return routingDataSource
    }

    @Primary
    @Bean
    fun dataSource(
        @Qualifier("routingDataSource") routingDataSource: DataSource,
    ): DataSource =
        LazyConnectionDataSourceProxy(routingDataSource)

    companion object {
        const val MASTER_DATA_SOURCE = "masterDataSource"
        const val SLAVE_DATA_SOURCE = "slaveDataSource"
        private const val MASTER = "master"
        private const val SLAVE = "slave"
    }
}
