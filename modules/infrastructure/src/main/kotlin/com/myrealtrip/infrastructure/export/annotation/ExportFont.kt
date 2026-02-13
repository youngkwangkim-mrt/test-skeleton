package com.myrealtrip.infrastructure.export.annotation

/**
 * Export 폰트 Enum
 *
 * @property fontName 폰트 이름 (빈 문자열은 Excel 기본 폰트 사용)
 */
enum class ExportFont(val fontName: String) {
    // 기본
    DEFAULT(""),

    // 영문 폰트
    ARIAL("Arial"),
    CALIBRI("Calibri"),
    TIMES_NEW_ROMAN("Times New Roman"),
    VERDANA("Verdana"),
    CONSOLAS("Consolas"),

    // 한글 폰트
    MALGUN_GOTHIC("맑은 고딕"),
    NANUM_GOTHIC("나눔고딕"),
    NANUM_MYEONGJO("나눔명조"),
    GULIM("굴림"),
    DOTUM("돋움"),
}
