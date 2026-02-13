package com.myrealtrip.infrastructure.export.annotation

/**
 * Excel 시트 최대 행(1,048,576) 초과 시 처리 전략
 */
enum class OverflowStrategy {

    /**
     * 여러 시트로 분할 (Sheet1, Sheet1 (2), Sheet1 (3), ...)
     */
    MULTI_SHEET,

    /**
     * 예외 발생
     */
    EXCEPTION,

    /**
     * CSV 파일로 대체 출력 (미구현, 예약)
     */
    CSV_FALLBACK,
}
