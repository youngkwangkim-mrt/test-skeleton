# 엑셀 다운로드 기능 기획서

## 1. 개요

### 1.1 배경 및 목적

조회 데이터를 엑셀/CSV로 내보내 분석, 보고서 작성, 공유에 활용한다.

**기대 효과**:
- DTO에 어노테이션만 추가하면 엑셀 다운로드 구현 완료
- 100만 건 이상도 OOM 없이 처리
- 일관된 포맷과 스타일 유지

### 1.2 Quick Start

```kotlin
// 1. DTO에 어노테이션 추가
@ExportSheet(name = "주문목록")
data class OrderExportDto(
    @ExportColumn(header = "주문번호", order = 1)
    val orderNo: String,
    @ExportColumn(header = "금액", order = 2, format = "#,##0")
    val amount: BigDecimal,
)

// 2. Controller에서 Export 호출
excelExporter.export(orders, OrderExportDto::class, response.outputStream)
```

---

## 2. 요구사항

### 2.1 기능 요구사항

| 번호 | 요구사항 | 설명 |
|------|---------|------|
| FR-01 | DTO 기반 변환 | DTO 리스트 → 엑셀/CSV 변환 |
| FR-02 | 어노테이션 설정 | 헤더명, 볼드, 색상, 너비를 어노테이션으로 지정 |
| FR-03 | 타입별 포맷 | 숫자, 문자, Boolean, 날짜에 맞는 셀 포맷 적용 |
| FR-04 | 셀 스타일링 | 헤더/바디 각각 배경색, 볼드 등 스타일 적용 |
| FR-05 | 컬럼 순서 | 어노테이션으로 출력 순서 지정 |
| FR-06 | CSV Export | 동일 어노테이션으로 CSV 지원 (행 제한 없음) |

### 2.2 비기능 요구사항

| 번호 | 요구사항 | 목표 |
|------|---------|------|
| NFR-01 | 대용량 처리 | 100만 건+ OOM 없이 처리 |
| NFR-02 | 메모리 효율 | 스트리밍 방식, 힙 256MB 이내 |
| NFR-03 | 응답 시간 | 10만 건 기준 30초 이내 |
| NFR-04 | 비동기 처리 | 50만 건+ 비동기 다운로드 |

---

## 3. 기술 스택

### 3.1 라이브러리 선정

**Apache POI SXSSF** 사용

| 고려 항목 | POI XSSF | POI SXSSF | FastExcel | EasyExcel |
|----------|----------|-----------|-----------|-----------|
| 대용량 | X | O (스트리밍) | O | O |
| 스타일링 | O | O (제한적) | △ | △ |
| Spring 호환 | O | O | O | O |
| 문서/커뮤니티 | O | O | △ | △ |
| 커스터마이징 | O | O | △ | X |

SXSSF는 지정된 행 수만 메모리에 유지하고 나머지는 임시 파일로 flush한다.

### 3.2 제약사항

| 제약사항 | 설명 |
|---------|------|
| 쓰기 전용 | 읽기 불가, 생성 후 수정 불가 |
| 순차 쓰기 | 임의 행 접근 불가 |
| 스타일 제한 | 복잡한 스타일 일부 미지원 |

---

## 4. 설계

### 4.1 전체 아키텍처

```
Controller
    │
    ▼
DataExporter (Excel/CSV Export)
    │
    ├── ColumnMetaExtractor (어노테이션 파싱)
    ├── ValueConverter (타입별 변환)
    └── CellStyleFactory (스타일 생성)
```

### 4.2 모듈 구조

`infrastructure` 모듈에 배치

```
modules/infrastructure/
└── src/main/kotlin/com/myrealtrip/infrastructure/export/
    ├── annotation/
    │   ├── ExportColumn.kt
    │   ├── ExportSheet.kt
    │   ├── ExportCellStyle.kt
    │   ├── ExportColor.kt
    │   ├── ExportAlignment.kt
    │   └── OverflowStrategy.kt
    ├── DataExporter.kt              # Export 공통 인터페이스
    ├── excel/
    │   ├── ExcelExporter.kt
    │   └── CellStyleFactory.kt
    ├── csv/
    │   └── CsvExporter.kt
    ├── ColumnMetaExtractor.kt
    └── ValueConverter.kt
```

> **Note**: 어노테이션명 `Export*`로 통일 (Excel/CSV 공용)

**의존성**: `bootstrap` → `application` → `infrastructure` → `common`

### 4.3 처리 흐름

**동기 처리 (50만 건 미만)**:

```
1. Controller → DTO 리스트 + OutputStream 전달
2. SXSSF Workbook 생성 (윈도우: 500행)
3. @ExportColumn 어노테이션 정보 추출
4. 헤더 행 생성 (스타일 적용)
5. 데이터 행 순차 생성 (타입 변환)
6. OutputStream 출력 → close()
```

**대용량 처리 (청크 기반)**:

```
1. No-Offset Paging으로 1만 건씩 조회
2. 청크 → 엑셀 쓰기 → entityManager.clear()
3. 반복 → 완료 시 close()
```

---

## 5. 어노테이션 스펙

### 5.1 @ExportColumn

컬럼별 출력 설정

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExportColumn(
    val header: String,                                    // 헤더명 (필수)
    val order: Int = 0,                                    // 컬럼 순서
    val width: Int = -1,                                   // 컬럼 너비 (-1: 자동)
    val format: String = "",                               // 셀 포맷 (예: "#,##0")
    val headerStyle: ExportCellStyle = ExportCellStyle(),  // 헤더 스타일
    val bodyStyle: ExportCellStyle = ExportCellStyle(),    // 바디 스타일
)
```

### 5.2 @ExportSheet

시트 레벨 설정

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExportSheet(
    val name: String = "Sheet1",                 // 시트명
    val freezeHeader: Boolean = true,            // 헤더 행 고정
    val includeIndex: Boolean = false,           // 인덱스 컬럼 포함 (No.)
    val indexHeader: String = "No.",             // 인덱스 헤더명
    val indexWidth: Int = 6,                     // 인덱스 너비
    val overflowStrategy: OverflowStrategy = OverflowStrategy.MULTI_SHEET,
)
```

**인덱스 컬럼**: `includeIndex = true` → 첫 번째 컬럼(A열)에 1부터 행 번호 자동 추가

### 5.3 @ExportCellStyle

셀 스타일 설정

```kotlin
annotation class ExportCellStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: Short = -1,                              // -1: 기본값 11pt
    val fontColor: ExportColor = ExportColor.BLACK,
    val bgColor: ExportColor = ExportColor.NONE,
    val alignment: ExportAlignment = ExportAlignment.LEFT,
)
```

**기본값**:
- `headerStyle`: 볼드, 회색 배경, 가운데 정렬 (미지정 시)
- `bodyStyle`: 스타일 없음

### 5.4 Enum

**ExportColor**:

```kotlin
enum class ExportColor(val index: Short) {
    NONE(-1), BLACK(...), WHITE(...),
    // 그레이
    GREY_25(...), GREY_40(...), GREY_50(...),
    // 배경용
    LIGHT_BLUE(...), LIGHT_GREEN(...), LIGHT_YELLOW(...), LIGHT_ORANGE(...),
    // 글자용
    RED(...), DARK_RED(...), BLUE(...), DARK_BLUE(...), GREEN(...), ORANGE(...),
}
```

**ExportAlignment**:

```kotlin
enum class ExportAlignment { LEFT, CENTER, RIGHT }
```

**OverflowStrategy**:

```kotlin
enum class OverflowStrategy {
    MULTI_SHEET,   // 여러 시트로 분할 (Sheet1, Sheet2, ...)
    EXCEPTION,     // 예외 발생
    CSV_FALLBACK,  // CSV 파일로 대체 출력
}
```

> 엑셀 시트 최대: 1,048,576행. 초과 시 `OverflowStrategy`에 따라 처리

---

## 6. 타입 변환 규칙

| Kotlin 타입 | 셀 타입 | 기본 포맷 |
|-------------|--------|----------|
| `String` | STRING | - |
| `Int`, `Long` | NUMERIC | `#,##0` |
| `Double`, `BigDecimal` | NUMERIC | `#,##0.00` |
| `Boolean` | STRING | "Y" / "N" |
| `LocalDate` | NUMERIC | `yyyy-MM-dd` |
| `LocalDateTime` | NUMERIC | `yyyy-MM-dd HH:mm:ss` |
| `Enum` | STRING | `name()` |
| `null` | BLANK | - |

---

## 7. 사용 예시

### 7.1 기본 사용법

**DTO 정의**:

```kotlin
@ExportSheet(name = "주문목록", includeIndex = true)
data class OrderExportDto(
    @ExportColumn(header = "주문번호", order = 1, width = 15)
    val orderNo: String,

    @ExportColumn(header = "주문일시", order = 2, format = "yyyy-MM-dd HH:mm")
    val orderedAt: LocalDateTime,

    @ExportColumn(header = "고객명", order = 3)
    val customerName: String,

    @ExportColumn(
        header = "금액",
        order = 4,
        format = "#,##0",
        bodyStyle = ExportCellStyle(bold = true, bgColor = ExportColor.LIGHT_BLUE),
    )
    val amount: BigDecimal,

    @ExportColumn(
        header = "상태",
        order = 5,
        bodyStyle = ExportCellStyle(fontColor = ExportColor.RED),
    )
    val status: OrderStatus,
)
```

**Controller**:

```kotlin
@GetMapping("/excel")
fun downloadExcel(
    @RequestParam startDate: LocalDate,
    @RequestParam endDate: LocalDate,
    response: HttpServletResponse,
) {
    val orders = orderService.findOrders(startDate, endDate)
        .map { it.toExportDto() }

    response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    response.setHeader("Content-Disposition", "attachment; filename=\"orders.xlsx\"")

    excelExporter.export(orders, OrderExportDto::class, response.outputStream)
}
```

### 7.2 대용량 처리 (청크 기반)

`@QueryProjection` DTO를 직접 조회하여 변환 비용 제거

```kotlin
@GetMapping("/excel")
fun downloadLargeExcel(condition: OrderSearchCondition, response: HttpServletResponse) {
    response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    response.setHeader("Content-Disposition", "attachment; filename=\"orders.xlsx\"")

    excelExporter.exportWithChunks(
        clazz = OrderExportDto::class,
        outputStream = response.outputStream,
    ) { consumer ->
        // @QueryProjection DTO 직접 조회 → 변환 없이 바로 Export
        orderQueryRepository.fetchOrderDtosInChunks(condition, consumer)
    }
}
```

---

## 8. 성능 및 제약사항

### 8.1 성능 최적화

| 항목 | 방안 | 효과 |
|------|------|------|
| @QueryProjection | DTO 직접 조회 | 변환 비용 제거, clear() 불필요 |
| SXSSF 윈도우 | 500행만 메모리 유지 | 메모리 90%↓ |
| No-Offset Paging | `id > lastId` 방식 | 페이징 성능↑ |
| 청크 처리 | 1만 건 단위 반복 | 안정적 처리 |
| 스타일 캐싱 | CellStyle 재사용 | 64K 제한 회피 |
| 리플렉션 캐싱 | 메타 1회 추출 | CPU↓ |
| 임시 파일 정리 | `close()` 호출 | 디스크 누수 방지 |

**벤치마크 결과** (2025-01-17, M1 Mac 기준):

| 포맷 | 건수 | 시간 | 처리량 | 파일 크기 |
|------|------|------|--------|----------|
| Excel | 10K | 106ms | 94K rows/sec | 546KB |
| Excel | 100K | 961ms | 104K rows/sec | 5.3MB |
| Excel | 500K | 4,814ms | 104K rows/sec | 26.5MB |
| CSV | 10K | 18ms | 556K rows/sec | 1MB |
| CSV | 100K | 132ms | 758K rows/sec | 10.4MB |
| CSV | 500K | 911ms | 549K rows/sec | 53.7MB |

**메모리 사용량** (100K rows 기준):
- 데이터 생성: 43MB
- Excel Export: 11MB
- 총 사용량: 54MB

**청크 크기별 성능** (Excel 100K rows):

| Chunk Size | Time (ms) | Rows/sec |
|------------|-----------|----------|
| 100 | 3,792 | 26K |
| 500 | 2,353 | 42K |
| 1,000 | 1,961 | 51K |
| **5,000** | **946** | **106K** |
| 10,000 | 1,013 | 99K |

> **권장**: 청크 크기 5,000이 최적 성능

**rowAccessWindowSize 비교** (Excel 100K rows):

| Window Size | Time (ms) | Rows/sec |
|-------------|-----------|----------|
| 100 | 961 | 104K |
| 500 | 993 | 101K |
| 1,000 | 947 | 106K |
| 2,000 | 950 | 105K |

> **결론**: Window size는 100~2,000 범위에서 유의미한 차이 없음. 기본값 1,000 유지

**예상 리소스** (실측 기반):

| 건수 | 메모리 | 시간 (Excel) | 시간 (CSV) | 처리 |
|------|--------|-------------|------------|------|
| 1만 | ~15MB | ~0.1초 | ~0.02초 | 동기 |
| 10만 | ~55MB | ~1초 | ~0.15초 | 동기 |
| 50만 | ~150MB | ~5초 | ~1초 | 동기 |
| 100만 | ~300MB | ~10초 | ~2초 | 동기/비동기 |

### 8.2 제약사항

| 제약 | 설명 | 대응 |
|------|------|------|
| 행 제한 | xlsx 최대 1,048,576행 | `OverflowStrategy` |
| 쓰기 전용 | 생성 후 수정 불가 | XSSF (소량) |
| 임시 파일 | 디스크 필요 | /tmp 확보, close() |
| 스타일 제한 | Workbook당 64K | 캐싱 필수 |
| 동시 요청 | 리소스 부하 | 다운로드 수 제한 |
| 타임아웃 | 대용량 시 끊김 | 비동기 + 링크 |

---

## 9. 개발 로드맵

### 9.1 Phase 1-4

#### Phase 1: Core

기본 Excel/CSV Export

| 작업 | 산출물 |
|------|--------|
| 어노테이션 | `ExportColumn.kt`, `ExportSheet.kt` |
| 메타 추출 | `ColumnMetaExtractor.kt` |
| 타입 변환 | `ValueConverter.kt` |
| Excel/CSV | `ExcelExporter.kt`, `CsvExporter.kt` |

**완료**: 1만 건 Export 정상 동작

---

#### Phase 2: Styling

헤더/바디 스타일링 (Excel 전용)

| 작업 | 산출물 |
|------|--------|
| 스타일 어노테이션 | `ExportCellStyle.kt` |
| Enum | `ExportColor.kt`, `ExportAlignment.kt` |
| 스타일 팩토리 | `CellStyleFactory.kt` |
| 인덱스 컬럼 | `@ExportSheet.includeIndex` |

**완료**: headerStyle/bodyStyle 분리 적용, 인덱스 동작

---

#### Phase 3: Large Data

100만 건+ 처리

| 작업 | 산출물 |
|------|--------|
| 청크 Export | `exportWithChunks()` |
| QueryDSL | No-Offset Paging |
| 최적화 | SXSSF 윈도우 튜닝 |

**완료**: 100만 건 OOM 없이 처리, 힙 256MB 이내

---

#### Phase 4: Advanced

엣지 케이스 처리

| 작업 | 산출물 |
|------|--------|
| 행 초과 | `OverflowStrategy` |

| 멀티시트 | 시트 자동 분할 |
| 문서화 | README |

**완료**: 100만 건+ 멀티시트 분할 Export

---

**우선순위**: Phase 1~4 모두 **필수**

### 9.2 Backlog: 비동기 다운로드

초대용량 비동기 처리 (다음 Scope)

**처리 흐름**:

```
다운로드 요청 → DB 저장 (PENDING) → URL 즉시 응답
                    ↓
         비동기 Worker 파일 생성 (PROCESSING)
                    ↓
              S3 업로드 (COMPLETED)
                    ↓
              담당자 알림 전송
```

**주요 기능**:

| 기능 | 설명 |
|------|------|
| 중복 방지 | 해시 기반 1회만 실행 |
| 파일 보관 | 30일 보관 |
| 삭제 스케줄러 | 만료 파일 일 1회 삭제 |
| 상태 조회 | PENDING → PROCESSING → COMPLETED/FAILED |

**DB 스키마**:

```sql
create table excel_export_jobs (
    id              bigint primary key,
    request_hash    varchar(64) not null,
    status          varchar(20) not null,
    file_url        varchar(500),
    error_message   varchar(500),
    created_at      timestamp not null,
    completed_at    timestamp,
    expires_at      timestamp not null,

    constraint uk_excel_export_jobs_01 unique (request_hash, status)
);
```

---

## 부록

### A. QueryDSL Repository

#### A-1. @QueryProjection DTO 직접 조회 (권장)

Entity 대신 DTO를 직접 조회하여 **변환 비용 제거 + 필요 컬럼만 SELECT**

```kotlin
// DTO 정의
@ExportSheet(name = "주문목록")
data class OrderExportDto @QueryProjection constructor(
    @ExportColumn(header = "주문번호", order = 1)
    val orderNo: String,
    @ExportColumn(header = "주문일시", order = 2)
    val orderedAt: LocalDateTime,
    @ExportColumn(header = "고객명", order = 3)
    val customerName: String,
    @ExportColumn(header = "금액", order = 4, format = "#,##0")
    val amount: BigDecimal,
    @ExportColumn(header = "상태", order = 5)
    val status: OrderStatus,
) {
    // No-Offset Paging용 ID (Export 컬럼 아님)
    @Transient
    var id: Long = 0L
}
```

```kotlin
@Repository
class OrderQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {

    /**
     * @QueryProjection DTO 직접 조회 (권장)
     * - Entity 변환 없음 → 성능↑
     * - 영속성 컨텍스트 미사용 → entityManager.clear() 불필요
     */
    fun fetchOrderDtosInChunks(
        condition: OrderSearchCondition,
        chunkSize: Int = 10_000,
        consumer: (List<OrderExportDto>) -> Unit,
    ) {
        var lastId: Long? = null

        while (true) {
            val chunk = queryFactory
                .select(
                    QOrderExportDto(
                        order.orderNo,
                        order.orderedAt,
                        order.customer.name,
                        order.amount,
                        order.status,
                    )
                )
                .from(order)
                .join(order.customer, customer)
                .where(
                    order.orderedAt.between(condition.startDate, condition.endDate),
                    condition.status?.let { order.status.eq(it) },
                    lastId?.let { order.id.gt(it) }
                )
                .orderBy(order.id.asc())
                .limit(chunkSize.toLong())
                .fetch()
                .onEach { it.id = order.id }  // No-Offset용 ID 세팅

            if (chunk.isEmpty()) break

            consumer(chunk)
            lastId = chunk.last().id
            // entityManager.clear() 불필요 (DTO는 영속성 컨텍스트에 없음)
        }
    }
}
```

#### A-2. Entity 조회 후 변환

Entity 조회가 필요한 경우 (복잡한 연관관계, 지연로딩 등)

```kotlin
@Repository
class OrderQueryRepository(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) {

    fun fetchOrdersInChunks(
        condition: OrderSearchCondition,
        chunkSize: Int = 10_000,
        consumer: (List<Order>) -> Unit,
    ) {
        var lastId: Long? = null

        while (true) {
            val chunk = queryFactory
                .selectFrom(order)
                .join(order.customer, customer).fetchJoin()
                .where(
                    order.orderedAt.between(condition.startDate, condition.endDate),
                    order.status.eq(condition.status),
                    lastId?.let { order.id.gt(it) }
                )
                .orderBy(order.id.asc())
                .limit(chunkSize.toLong())
                .fetch()

            if (chunk.isEmpty()) break

            consumer(chunk)
            lastId = chunk.last().id
            entityManager.clear()  // Entity는 clear 필요
        }
    }
}
```

#### 비교

| 항목 | @QueryProjection DTO | Entity 조회 |
|------|---------------------|-------------|
| 변환 비용 | 없음 | Entity → DTO 변환 필요 |
| SELECT 컬럼 | 필요한 것만 | 전체 컬럼 |
| entityManager.clear() | 불필요 | 필요 |
| 사용 케이스 | 단순 조회/Export | 복잡한 연관관계 |

### B. CellStyleFactory

```kotlin
class CellStyleFactory(private val workbook: SXSSFWorkbook) {

    private val styleCache = mutableMapOf<String, CellStyle>()
    private val fontCache = mutableMapOf<String, Font>()

    fun getStyle(style: ExportCellStyle, format: String? = null): CellStyle {
        val key = buildStyleKey(style, format)
        return styleCache.getOrPut(key) {
            workbook.createCellStyle().apply {
                if (style.bgColor != ExportColor.NONE) {
                    fillForegroundColor = style.bgColor.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }
                if (style.bold || style.italic || style.fontSize > 0 || style.fontColor != ExportColor.BLACK) {
                    setFont(getFont(style))
                }
                alignment = when (style.alignment) {
                    ExportAlignment.LEFT -> HorizontalAlignment.LEFT
                    ExportAlignment.CENTER -> HorizontalAlignment.CENTER
                    ExportAlignment.RIGHT -> HorizontalAlignment.RIGHT
                }
                if (!format.isNullOrBlank()) {
                    dataFormat = workbook.createDataFormat().getFormat(format)
                }
            }
        }
    }

    fun getDefaultHeaderStyle(): CellStyle {
        return getStyle(
            ExportCellStyle(
                bold = true,
                bgColor = ExportColor.GREY_25,
                alignment = ExportAlignment.CENTER,
            )
        )
    }

    private fun getFont(style: ExportCellStyle): Font {
        val key = "font_${style.bold}_${style.italic}_${style.fontSize}_${style.fontColor}"
        return fontCache.getOrPut(key) {
            workbook.createFont().apply {
                bold = style.bold
                italic = style.italic
                if (style.fontSize > 0) fontHeightInPoints = style.fontSize
                if (style.fontColor != ExportColor.BLACK) color = style.fontColor.index
            }
        }
    }

    private fun buildStyleKey(style: ExportCellStyle, format: String?) =
        "style_${style.bgColor}_${style.fontColor}_${style.bold}_${style.italic}_${style.fontSize}_${style.alignment}_$format"
}
```

### C. ExcelExportService

```kotlin
@Service
class ExcelExportService(
    private val entityManager: EntityManager,
) {

    fun <T : Any> export(data: List<T>, clazz: KClass<T>, outputStream: OutputStream) {
        createWorkbook(clazz, outputStream) { sheet, styleFactory, columnMetas ->
            data.forEachIndexed { index, item ->
                createDataRow(sheet, index + 1, item, columnMetas, styleFactory)
            }
        }
    }

    fun <T : Any> exportWithChunks(
        clazz: KClass<T>,
        outputStream: OutputStream,
        chunkFetcher: (consumer: (List<T>) -> Unit) -> Unit,
    ) {
        createWorkbook(clazz, outputStream) { sheet, styleFactory, columnMetas ->
            var rowIndex = 1
            chunkFetcher { chunk ->
                chunk.forEach { item ->
                    createDataRow(sheet, rowIndex++, item, columnMetas, styleFactory)
                }
                entityManager.clear()
            }
        }
    }

    private fun <T : Any> createWorkbook(
        clazz: KClass<T>,
        outputStream: OutputStream,
        dataWriter: (SXSSFSheet, CellStyleFactory, List<ColumnMeta>) -> Unit,
    ) {
        SXSSFWorkbook(500).use { workbook ->
            val sheetMeta = extractSheetMeta(clazz)
            val columnMetas = extractColumnMetas(clazz)
            val styleFactory = CellStyleFactory(workbook)

            val sheet = workbook.createSheet(sheetMeta.name)
            createHeaderRow(sheet, columnMetas, styleFactory)

            if (sheetMeta.freezeHeader) sheet.createFreezePane(0, 1)

            dataWriter(sheet, styleFactory, columnMetas)
            setColumnWidths(sheet, columnMetas)

            workbook.write(outputStream)
            workbook.close()
        }
    }
}
```

---

## 변경 이력

| 버전 | 날짜 | 내용 |
|------|------|------|
| 1.0 | 2025-01-17 | 최초 작성 |
| 1.1 | 2025-01-17 | 구조 개선, 문장 다듬기 |
| 1.2 | 2025-01-17 | @QueryProjection DTO 직접 조회 패턴 추가 |
| 1.3 | 2025-01-17 | 벤치마크 결과 추가 (Phase 3) |
