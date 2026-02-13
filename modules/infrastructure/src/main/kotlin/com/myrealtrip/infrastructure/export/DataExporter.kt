package com.myrealtrip.infrastructure.export

import java.io.OutputStream
import kotlin.reflect.KClass

/**
 * 데이터 Export 공통 인터페이스
 */
interface DataExporter {

    /**
     * 데이터 리스트를 Export
     *
     * @param data Export 대상 데이터
     * @param clazz DTO 클래스 (어노테이션 추출용)
     * @param outputStream 출력 스트림
     */
    fun <T : Any> export(
        data: List<T>,
        clazz: KClass<T>,
        outputStream: OutputStream,
    )

    /**
     * 청크 단위로 데이터를 Export (대용량)
     *
     * @param clazz DTO 클래스 (어노테이션 추출용)
     * @param outputStream 출력 스트림
     * @param chunkFetcher 청크 데이터 제공 함수
     */
    fun <T : Any> exportWithChunks(
        clazz: KClass<T>,
        outputStream: OutputStream,
        chunkFetcher: (consumer: (List<T>) -> Unit) -> Unit,
    )
}
