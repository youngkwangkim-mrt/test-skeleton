---
name: Layer Architecture
description: Layer structure, responsibilities, DTO flow, and conventions based on Holiday feature reference implementation
---

# Layer Architecture

## Overview

이 프로젝트는 4-Layer Architecture를 따릅니다.

> **Core Rule**: 상위 레이어만 하위 레이어에 의존한다. 역방향 의존은 금지.

```
Bootstrap (Controller → Facade)
    ↓
Domain Application (QueryApplication / CommandApplication)
    ↓
Domain Service (Business Logic)
    ↓
Domain Repository (JpaRepository / QueryRepository)
    ↓
Domain Entity (JPA Entity)
```

---

## Layer 1: Bootstrap (HTTP Entry Point)

> **Module**: `modules/bootstrap/{app-name}/`

HTTP 요청/응답을 처리하는 진입점. API DTO 변환과 라우팅만 담당.

### Controller

| Rule | Description |
|------|-------------|
| 위치 | `{appname}/api/{Feature}Controller.kt` |
| 역할 | HTTP 엔드포인트 정의, 요청 라우팅 |
| 의존 | Facade만 주입 (Service, Application 직접 주입 금지) |
| 반환 | `ResponseEntity<ApiResource<T>>` |
| 변환 | API Request DTO → Domain Request DTO (간단한 변환만) |

```kotlin
@RestController
@RequestMapping("/api/holidays")
class HolidayController(
    private val holidayFacade: HolidayFacade,  // Facade만 주입
) {
    @GetMapping("/{year}")
    fun getByYear(@PathVariable year: Int): ResponseEntity<ApiResource<HolidaysResponse>> =
        ApiResource.success(holidayFacade.findByYear(year))

    @PostMapping
    fun create(@RequestBody request: CreateHolidayApiRequest): ResponseEntity<ApiResource<HolidayDto>> {
        val createRequest = CreateHolidayRequest(request.holidayDate, request.name)
        return ApiResource.success(holidayFacade.create(createRequest))
    }
}
```

### Facade

| Rule | Description |
|------|-------------|
| 위치 | `{appname}/facade/{Feature}Facade.kt` |
| 역할 | API DTO ↔ Domain DTO 변환, Application 호출 조합 |
| 의존 | QueryApplication, CommandApplication 주입 |
| 어노테이션 | `@Component` |

```kotlin
@Component
class HolidayFacade(
    private val holidayQueryApplication: HolidayQueryApplication,
    private val holidayCommandApplication: HolidayCommandApplication,
) {
    // Domain DTO → API DTO 변환
    fun findByYear(year: Int): HolidaysResponse {
        val holidays = holidayQueryApplication.findByYear(year)
        return HolidaysResponse.from(holidays)
    }

    fun create(request: CreateHolidayRequest): HolidayDto {
        val holiday = holidayCommandApplication.create(request)
        return HolidayDto.from(holiday)
    }
}
```

### API DTOs

| Rule | Description |
|------|-------------|
| Request 위치 | `{appname}/dto/request/{Feature}ApiRequest.kt` |
| Response 위치 | `{appname}/dto/response/{Feature}ApiResponse.kt` |
| 네이밍 | Request: `{Action}{Feature}ApiRequest`, Response: `{Feature}Dto`, `{Feature}sResponse` |

---

## Layer 2: Domain Application (Orchestration)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.application`

트랜잭션 경계를 관리하는 얇은 위임 계층. Query/Command 분리 (CQRS-light).

### QueryApplication (조회)

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/application/{Feature}QueryApplication.kt` |
| 어노테이션 | `@Service`, `@Transactional(readOnly = true)` (클래스 레벨) |
| 의존 | Service만 주입 |
| 반환 | Domain DTO (`{Feature}Info`) |

```kotlin
@Service
@Transactional(readOnly = true)
class HolidayQueryApplication(
    private val holidayService: HolidayService,
) {
    fun findByYear(year: Int): List<HolidayInfo> =
        holidayService.findByYear(year)

    fun findById(id: Long): HolidayInfo =
        holidayService.findById(id)
}
```

### CommandApplication (생성/수정/삭제)

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/application/{Feature}CommandApplication.kt` |
| 어노테이션 | `@Service`, `@Transactional` (클래스 레벨) |
| 의존 | Service만 주입 |
| 반환 | Domain DTO (`{Feature}Info`) |

```kotlin
@Service
@Transactional
class HolidayCommandApplication(
    private val holidayService: HolidayService,
) {
    fun create(request: CreateHolidayRequest): HolidayInfo =
        holidayService.create(request)

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo =
        holidayService.update(id, request)

    fun delete(id: Long) =
        holidayService.delete(id)
}
```

---

## Layer 3: Domain Service (Business Logic)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.service`

핵심 비즈니스 로직. Entity 조작과 Domain DTO 변환 담당.

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/service/{Feature}Service.kt` |
| 어노테이션 | `@Service` |
| 의존 | Repository만 주입 (JpaRepository, QueryRepository) |
| 반환 | Domain DTO (`{Feature}Info`) |
| 변환 | Entity → Domain DTO (`entity.toInfo()`) |

```kotlin
@Service
class HolidayService(
    private val holidayJpaRepository: HolidayJpaRepository,
) {
    fun findByYear(year: Int): List<HolidayInfo> =
        holidayJpaRepository.findByYear(year).map { it.toInfo() }

    fun create(request: CreateHolidayRequest): HolidayInfo {
        val holiday = Holiday.create(request.holidayDate, request.name)
        return holidayJpaRepository.save(holiday).toInfo()
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        val holiday = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }
        holiday.update(request.holidayDate, request.name)
        return holiday.toInfo()
    }

    fun delete(id: Long) {
        val holiday = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }
        holidayJpaRepository.delete(holiday)
    }
}
```

---

## Layer 4: Domain Repository (Persistence)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.repository`

데이터 접근 계층. Spring Data JPA와 QueryDSL 두 가지 방식.

### JpaRepository

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/repository/{Feature}JpaRepository.kt` |
| 방식 | Spring Data JPA interface |
| 용도 | 단순 CRUD, 파생 쿼리, 커스텀 `@Query` |

```kotlin
interface HolidayJpaRepository : JpaRepository<Holiday, Long> {
    fun findByHolidayDate(holidayDate: LocalDate): List<Holiday>

    @Query("select h from Holiday h where year(h.holidayDate) = :year order by h.holidayDate")
    fun findByYear(@Param("year") year: Int): List<Holiday>
}
```

### QueryRepository

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/repository/{Feature}QueryRepository.kt` |
| 방식 | QueryDSL, `QuerydslRepositorySupport` 상속 |
| 용도 | 동적 조건, 페이징, 복잡 조인 |
| 네이밍 | `fetch` 접두사 필수 (see `22_querydsl.md`) |

```kotlin
@Repository
class HolidayQueryRepository : QuerydslRepositorySupport(Holiday::class.java) {

    fun fetchPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return applyPagination(
            pageable,
            contentQuery = { queryFactory ->
                queryFactory.selectFrom(holiday)
                    .where(holiday.holidayDate.year().eq(year))
                    .orderBy(holiday.holidayDate.asc())
            },
            countQuery = { queryFactory ->
                queryFactory.select(holiday.count()).from(holiday)
                    .where(holiday.holidayDate.year().eq(year))
            },
        ).map { it.toInfo() }
    }
}
```

---

## Domain Entity & DTO

> **Module**: `modules/domain/`

### Entity

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/entity/{Feature}.kt` |
| 상속 | `BaseTimeEntity` 또는 `BaseEntity` |
| 불변성 | private setter, `update()` 메서드로 변경 |
| 팩토리 | `companion object` 내 `create()` 메서드 |
| 변환 | `toInfo()` 메서드로 Domain DTO 변환 |

```kotlin
@Entity
@Table(name = "holidays")
class Holiday(
    holidayDate: LocalDate,
    name: String,
    id: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = id ?: 0

    @Column(nullable = false)
    var holidayDate: LocalDate = holidayDate
        private set

    @Column(nullable = false, length = 50)
    var name: String = name
        private set

    fun update(holidayDate: LocalDate, name: String) {
        this.holidayDate = holidayDate
        this.name = name
    }

    fun toInfo() = HolidayInfo(id = id, holidayDate = holidayDate, name = name)

    companion object {
        fun create(holidayDate: LocalDate, name: String) = Holiday(holidayDate = holidayDate, name = name)
    }
}
```

### Domain DTOs

| Rule | Description |
|------|-------------|
| 위치 | `domain/{feature}/dto/` |
| 조회 DTO | `{Feature}Info` — Entity에서 변환된 읽기 전용 DTO |
| 생성 요청 | `Create{Feature}Request` |
| 수정 요청 | `Update{Feature}Request` |
| 예외 | `{Feature}NotFoundException` (extends `KnownException`) |

```kotlin
data class HolidayInfo(val id: Long, val holidayDate: LocalDate, val name: String)

data class CreateHolidayRequest(val holidayDate: LocalDate, val name: String)

data class UpdateHolidayRequest(val holidayDate: LocalDate, val name: String)

class HolidayNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND, "Holiday not found: $id"
)
```

---

## DTO Flow (Data Conversion Points)

```
[HTTP Request JSON]
    ↓ deserialize
CreateHolidayApiRequest (Bootstrap: dto/request/)
    ↓ Controller에서 변환
CreateHolidayRequest (Domain: dto/)
    ↓ Facade → Application → Service
Holiday Entity (Domain: entity/)
    ↓ entity.toInfo()
HolidayInfo (Domain: dto/)
    ↓ Facade에서 변환 (HolidayDto.from())
HolidayDto (Bootstrap: dto/response/)
    ↓ ApiResource.success() wrapping
[HTTP Response JSON]
```

### DTO Conversion Rules

| Conversion | Where | Method |
|------------|-------|--------|
| API Request → Domain Request | Controller | 생성자 직접 호출 |
| Entity → Domain DTO | Service | `entity.toInfo()` |
| Domain DTO → API Response | Facade | `ResponseDto.from(domainDto)` |

> **IMPORTANT**: Controller는 Entity를 직접 다루지 않는다. Facade는 Entity를 직접 다루지 않는다.

---

## Dependency Injection Rules

| Layer | 주입 대상 | 금지 |
|-------|-----------|------|
| Controller | Facade만 | Service, Application, Repository 직접 주입 금지 |
| Facade | QueryApplication, CommandApplication | Service, Repository 직접 주입 금지 |
| Application | Service만 | Repository 직접 주입 금지 |
| Service | JpaRepository, QueryRepository | 다른 Service 주입 가능 (동일 레이어) |

```
Controller → Facade → Application → Service → Repository
    (각 레이어는 바로 아래 레이어만 주입)
```

---

## Transaction Rules

| Layer | Transaction | Reason |
|-------|-------------|--------|
| Controller | 없음 | HTTP 계층, 트랜잭션 관리 안 함 |
| Facade | 없음 | DTO 변환만, 트랜잭션 불필요 |
| QueryApplication | `@Transactional(readOnly = true)` | 읽기 최적화 |
| CommandApplication | `@Transactional` | 쓰기 트랜잭션 |
| Service | 없음 (Application에서 전파) | 중복 트랜잭션 방지 |

---

## Anti-Patterns

| Anti-Pattern | Problem | Correct |
|--------------|---------|---------|
| Controller에서 Service 직접 호출 | Facade 우회, DTO 변환 누락 | Controller → Facade → Application |
| Facade에서 Repository 호출 | 레이어 건너뛰기 | Facade → Application → Service → Repository |
| Service에서 API DTO 반환 | Bootstrap 의존성 역전 | Service는 Domain DTO만 반환 |
| Entity를 API 응답으로 직접 반환 | 내부 구조 노출 | Entity → Info → Dto 변환 |
| Application에서 비즈니스 로직 구현 | Application은 위임만 | 비즈니스 로직은 Service에 |
| Service에 `@Transactional` 추가 | Application과 중복 | Application에서만 트랜잭션 관리 |

---

## Summary Checklist

새 기능 추가 시 확인:

- [ ] Controller는 Facade만 주입하고 있는가
- [ ] Facade는 Application만 주입하고 있는가
- [ ] Application은 Service만 주입하고 있는가
- [ ] `@Transactional`은 Application 레벨에만 있는가
- [ ] QueryApplication은 `readOnly = true`인가
- [ ] Entity → Info 변환은 Service에서 수행하는가
- [ ] Info → API DTO 변환은 Facade에서 수행하는가
- [ ] API Request → Domain Request 변환은 Controller에서 수행하는가
- [ ] Domain DTO가 Bootstrap 모듈에 의존하지 않는가
