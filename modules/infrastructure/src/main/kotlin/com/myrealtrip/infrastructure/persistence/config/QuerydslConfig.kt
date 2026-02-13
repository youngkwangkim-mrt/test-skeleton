package com.myrealtrip.infrastructure.persistence.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * QueryDSL 설정
 */
@Configuration
class QuerydslConfig(
    @PersistenceContext private val entityManager: EntityManager,
) {

    @Bean
    @ConditionalOnMissingBean
    fun jpaQueryFactory(): JPAQueryFactory = JPAQueryFactory(entityManager)
}