package com.myrealtrip.domain.common.querydsl

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.BooleanPath
import com.querydsl.core.types.dsl.DateTimePath
import com.querydsl.core.types.dsl.EnumPath
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.core.types.dsl.StringPath
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * QueryDSL 검색 조건 생성 유틸리티
 *
 * 각 함수는 값이 null이거나 비어있으면 null을 반환하여
 * QueryDSL의 BooleanBuilder.and(null) 동작으로 조건을 무시할 수 있다.
 */
object QuerydslExpressions {

    // =============================================================================
    // equals
    // =============================================================================

    fun eq(path: StringPath, right: String?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.eq(it) }

    fun eq(path: BooleanPath, right: Boolean?): BooleanExpression? =
        right?.let { path.eq(it) }

    fun <S : Enum<S>> eq(path: EnumPath<S>, right: S?): BooleanExpression? =
        right?.let { path.eq(it) }

    fun <S> eq(path: NumberPath<S>, right: S?): BooleanExpression?
            where S : Number, S : Comparable<*> =
        right?.let { path.eq(it) }

    // =============================================================================
    // comparison (number)
    // =============================================================================

    fun <S> gt(path: NumberPath<S>, right: S?): BooleanExpression?
            where S : Number, S : Comparable<*> =
        right?.let { path.gt(it) }

    fun <S> gte(path: NumberPath<S>, right: S?): BooleanExpression?
            where S : Number, S : Comparable<*> =
        right?.let { path.goe(it) }

    fun <S> lt(path: NumberPath<S>, right: S?): BooleanExpression?
            where S : Number, S : Comparable<*> =
        right?.let { path.lt(it) }

    fun <S> lte(path: NumberPath<S>, right: S?): BooleanExpression?
            where S : Number, S : Comparable<*> =
        right?.let { path.loe(it) }

    // =============================================================================
    // string matching
    // =============================================================================

    fun startsWith(path: StringPath, right: String?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.startsWith(it) }

    fun startsWithIgnoreCase(path: StringPath, right: String?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.startsWithIgnoreCase(it) }

    fun contains(path: StringPath, right: String?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.contains(it) }

    fun containsIgnoreCase(path: StringPath, right: String?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.containsIgnoreCase(it) }

    fun containsIgnoreCaseAndSpace(path: StringPath, right: String?): BooleanExpression? {
        if (right.isNullOrEmpty()) return null

        val cleaned = right.replace("\\s+".toRegex(), "").lowercase()
        return Expressions.stringTemplate("replace(lower({0}), ' ', '')", path).contains(cleaned)
    }

    // =============================================================================
    // in
    // =============================================================================

    fun `in`(path: StringPath, right: Collection<String>?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.`in`(it) }

    fun inIgnoreCase(path: StringPath, right: Collection<String>?): BooleanExpression? {
        if (right.isNullOrEmpty()) return null

        return Expressions.stringTemplate("lower({0})", path)
            .`in`(right.map { it.lowercase() }.toSet())
    }

    fun <S : Enum<S>> `in`(path: EnumPath<S>, right: Collection<S>?): BooleanExpression? =
        right?.takeIf { it.isNotEmpty() }?.let { path.`in`(it) }

    // =============================================================================
    // boolean
    // =============================================================================

    fun isTrue(path: BooleanPath): BooleanExpression = path.isTrue

    fun isFalse(path: BooleanPath): BooleanExpression = path.isFalse

    // =============================================================================
    // date / datetime range
    // =============================================================================

    fun dateBetween(
        path: DateTimePath<LocalDate>,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): BooleanExpression? = when {
        startDate != null && endDate != null -> path.between(startDate, endDate)
        startDate != null -> path.after(startDate)
        endDate != null -> path.before(endDate)
        else -> null
    }

    fun dateTimeBetween(
        path: DateTimePath<LocalDateTime>,
        startDt: LocalDateTime?,
        endDt: LocalDateTime?,
    ): BooleanExpression? = when {
        startDt != null && endDt != null -> path.between(startDt, endDt)
        startDt != null -> path.after(startDt)
        endDt != null -> path.before(endDt)
        else -> null
    }
}
