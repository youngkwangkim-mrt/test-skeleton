package com.myrealtrip.domain.common.querydsl

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport
import org.springframework.data.jpa.repository.support.Querydsl
import org.springframework.data.querydsl.SimpleEntityPathResolver
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository
import org.springframework.util.Assert

/**
 * QueryDSL RepositorySupport
 *
 * QueryDSL 기반 Repository 구현을 위한 추상 클래스.
 * 페이징, select, selectFrom 등 공통 기능을 제공한다.
 */
@Repository
abstract class QuerydslRepositorySupport(
    private val domainClass: Class<*>,
) {

    private lateinit var querydsl: Querydsl
    private lateinit var entityManager: EntityManager
    private lateinit var queryFactory: JPAQueryFactory

    @Autowired
    fun setEntityManager(entityManager: EntityManager, jpaQueryFactory: JPAQueryFactory) {
        Assert.notNull(entityManager, "EntityManager must not be null!")

        val entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager)
        val path = SimpleEntityPathResolver.INSTANCE.createPath(entityInformation.javaType)

        this.entityManager = entityManager
        this.queryFactory = jpaQueryFactory
        this.querydsl = Querydsl(entityManager, PathBuilder(path.type, path.metadata))
    }

    @PostConstruct
    fun validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!")
        Assert.notNull(querydsl, "Querydsl must not be null!")
        Assert.notNull(queryFactory, "QueryFactory must not be null!")
    }

    protected fun getQueryFactory(): JPAQueryFactory = queryFactory

    protected fun getQuerydsl(): Querydsl = querydsl

    protected fun getEntityManager(): EntityManager = entityManager

    /**
     * 조회
     *
     * @param expr 조회 표현식
     * @return JPAQuery
     */
    protected fun <T> select(expr: Expression<T>): JPAQuery<T> = getQueryFactory().select(expr)

    /**
     * 조회
     *
     * @param from 조회 대상
     * @return JPAQuery
     */
    protected fun <T> selectFrom(from: EntityPath<T>): JPAQuery<T> = getQueryFactory().selectFrom(from)

    /**
     * 페이징 조회
     *
     * @param pageable     pageable
     * @param contentQuery 조회 쿼리
     * @param countQuery   count 쿼리
     * @return 페이징 처리 결과
     */
    protected fun <T : Any> applyPagination(
        pageable: Pageable,
        contentQuery: (JPAQueryFactory) -> JPAQuery<T>,
        countQuery: (JPAQueryFactory) -> JPAQuery<Long>,
    ): Page<T> {
        val jpaContentQuery = contentQuery(getQueryFactory())
        val content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch()

        val jpaCountQuery = countQuery(getQueryFactory())

        return PageableExecutionUtils.getPage(content, pageable) { jpaCountQuery.fetchOne() ?: 0L }
    }
}
