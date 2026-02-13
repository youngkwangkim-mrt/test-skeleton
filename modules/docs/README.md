# REST Docs Module

API 문서 생성 모듈입니다. Spring REST Docs 스니펫과 AsciiDoc 파일을 HTML 문서로 변환합니다.

## Structure

```
modules/docs/
├── build.gradle.kts              # Asciidoctor 설정
├── README.md
└── src/docs/asciidoc/
    ├── index.adoc                # 문서 홈 (메인 페이지)
    ├── .templates/               # 공통 템플릿
    │   ├── template-api-docs.adoc          # API 문서 템플릿 (복사해서 사용)
    │   ├── template-api-req-res.adoc       # 공통 응답 형식 (include용)
    │   ├── template-page-format.adoc       # 페이지 응답 형식 (include용)
    │   └── template-no-offset-page-format.adoc  # No-Offset 페이지 형식 (include용)
    └── {app-name}/               # 앱별 API 문서
        └── {domain}/
            └── {domain}-{type}-v1.adoc
```

## Usage

### Build (Recommended)

```bash
# 전체 빌드 (clean → test → snippets 복사 → HTML 생성)
./gradlew clean :modules:docs:build
```

### Individual Tasks

```bash
# 문서 생성 (test → snippets 복사 → HTML 생성)
./gradlew :modules:docs:docs

# AsciiDoc → HTML 변환만 (테스트 없이)
./gradlew :modules:docs:asciidoctor

# 스니펫 복사만
./gradlew :modules:docs:copySnippets
```

### Output Location

```
modules/docs/build/docs/
├── index.html                         # 문서 홈
└── common-api-app/
    └── holiday/
        ├── holiday-query-v1.html      # Holiday 조회 API
        └── holiday-command-v1.html    # Holiday CUD API
```

## Task Dependencies

```
:modules:bootstrap:common-api-app:test
:modules:bootstrap:skeleton-api-app:test
        ↓
  copySnippets (스니펫을 docs 모듈로 복사)
        ↓
  asciidoctor (AsciiDoc → HTML 변환)
        ↓
     docs
        ↓
     build
```

## Templates

### API 문서 템플릿

새 API 문서 작성 시 `.templates/template-api-docs.adoc` 템플릿을 복사하여 사용합니다.

**템플릿 위치**: `src/docs/asciidoc/.templates/template-api-docs.adoc`

### 템플릿 구조

```
= {API_NAME} API
= Overview
= APIs
  == 조회
    === 목록 조회
    === 상세 조회
  == 등록
    === 단건 등록
    === 대량 등록
  == 수정
    === 수정
  == 삭제
    === 삭제
```

### 플레이스홀더

| 플레이스홀더 | 설명 | 예시 |
|-------------|------|------|
| `{API_NAME}` | API 이름 | Holiday, User, Order |
| `{api_description}` | API 설명 | 공휴일 목록을 조회합니다 |
| `{test-class-name}` | DocsTest 클래스명 (kebab-case) | holiday-controller-docs-test |
| `{test-method-name}` | 테스트 메서드명 | get holidays by year |

### 스니펫 종류

| 스니펫 | 용도 |
|--------|------|
| `http-request.adoc` | HTTP 요청 예시 |
| `http-response.adoc` | HTTP 응답 예시 |
| `path-parameters.adoc` | Path 파라미터 설명 |
| `query-parameters.adoc` | Query 파라미터 설명 |
| `request-fields.adoc` | Request Body 필드 설명 |
| `response-fields-data.adoc` | Response data 필드 설명 |

## Adding New API Documentation

1. Bootstrap 앱 모듈에 REST Docs 테스트 작성
2. `.templates/template-api-docs.adoc` 복사하여 `{app-name}/{domain}/{domain}-{type}-v1.adoc` 생성
3. 플레이스홀더를 실제 값으로 변경
4. 필요 없는 API 섹션 삭제
5. `./gradlew clean :modules:docs:build` 실행

### Example

```asciidoc
= Holiday API - 조회
Holiday API 조회 연동 가이드
:doctype: book
:icons: font
:source-highlighter: highlightjs
:toc: left
:toclevels: 2

= Overview

.본 문서는 Holiday API 조회 기능을 연동하기 위한 문서입니다.
[NOTE]
--
- 공휴일 조회 기능을 제공합니다.
--

[[api-req-res]]
include::../../.templates/template-api-req-res.adoc[]

= APIs

== 조회

[[get-by-year]]
=== 연도별 공휴일 조회

==== Http Request

include::{snippets}/holiday-controller-docs-test/get holidays by year/http-request.adoc[]

==== Http Response

include::{snippets}/holiday-controller-docs-test/get holidays by year/http-response.adoc[]

.Response Fields `data`
include::{snippets}/holiday-controller-docs-test/get holidays by year/response-fields-data.adoc[]
```

## Dependencies

테스트 실행 대상 모듈:
- `:modules:bootstrap:common-api-app:test`
- `:modules:bootstrap:skeleton-api-app:test`

새 Bootstrap 앱 추가 시 `build.gradle.kts`의 `copySnippets` task에 의존성 추가 필요.

## Related Documentation

- [REST Docs 가이드](../../.docs/restdocs/index.adoc) - 전체 가이드 (목차)
  - [개요](../../.docs/restdocs/01-overview.adoc) - REST Docs 소개, 디렉토리 구조, 핵심 컴포넌트
  - [테스트 작성](../../.docs/restdocs/02-writing-tests.adoc) - DocsTest 작성 방법, Field DSL
  - [예시 및 AsciiDoc](../../.docs/restdocs/03-examples.adoc) - 샘플 코드 분석, AsciiDoc 작성
  - [참고 자료](../../.docs/restdocs/04-reference.adoc) - Best Practices, 트러블슈팅
- [API 응답 형식](../../.docs/architecture/07-api-response-format.adoc) - ApiResource 구조
