# Slack Notification Module

Kotlin DSL로 Slack 알림을 전송합니다. Block Kit과 Attachment를 지원합니다.

## Quick Start

```kotlin
// 1. 간단한 메시지
slackNotificationService.notify("#alerts", slackMessage {
    text("배포가 완료되었습니다!")
})

// 2. Block Kit 메시지
slackNotificationService.notify("#deployments", slackMessage {
    header("배포 알림")
    section { markdown("*앱:* `user-service`\n*버전:* `v2.3.1`") }
    divider()
    actions {
        button("로그 보기", ButtonStyle.PRIMARY) { url("https://logs.example.com") }
    }
})

// 3. 편의 메서드
slackNotificationService.notifySuccess("#alerts", "배포 완료", "user-service v2.3.1")
slackNotificationService.notifyError("#alerts", "배포 실패", exception)
slackNotificationService.notifyWarning("#alerts", "디스크 경고", "사용량 85%")

// 4. 색상 바 (Attachment)
slackNotificationService.notify("#alerts", slackMessage {
    attachment(SlackColor.SUCCESS) {
        section { markdown("*배포 완료*") }
    }
})
```

---

## 설정

### application.yml

```yaml
slack:
  enabled: true
  bot-token: ${SLACK_BOT_TOKEN}
  default-channel: "#general"
  retry:
    max-attempts: 3
    backoff-ms: 1000
    max-backoff-ms: 10000
```

### 설정 옵션

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enabled` | Boolean | `true` | `false`면 메시지를 전송하지 않음 |
| `bot-token` | String | (필수) | Bot User OAuth Token (`xoxb-...`) |
| `default-channel` | String | `null` | 설정하지 않으면 `notify()` 호출 시 채널 필수 |
| `retry.max-attempts` | Int | `3` | 최대 재시도 횟수 |
| `retry.backoff-ms` | Long | `1000` | 초기 백오프 (ms) |
| `retry.max-backoff-ms` | Long | `10000` | 최대 백오프 (ms) |

### 조건부 Bean 생성

`slack.enabled=true`(기본값)일 때만 Slack 관련 Bean이 생성됩니다.

```kotlin
@ConditionalOnProperty(name = ["slack.enabled"], havingValue = "true", matchIfMissing = true)
class SlackClientConfig
```

---

## API Reference

### SlackNotificationService

Slack 알림을 전송하는 고수준 서비스입니다.

#### notify

```kotlin
fun notify(channel: String? = null, message: SlackMessage): SlackResponse
```

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `channel` | String? | 채널. `null`이면 메시지의 채널 또는 기본 채널 사용 |
| `message` | SlackMessage | `slackMessage { }` DSL로 생성 |

**반환값**: `SlackResponse` (ts, channel 포함. 스레드 답장 시 ts 사용)

#### reply

```kotlin
fun reply(channel: String, threadTs: String, message: SlackMessage): SlackResponse
```

기존 메시지에 스레드로 답장합니다.

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `channel` | String | 채널 |
| `threadTs` | String | 원본 메시지의 ts (SlackResponse에서 획득) |
| `message` | SlackMessage | 답장 메시지 |

#### 편의 메서드

```kotlin
fun notifySuccess(channel: String? = null, title: String, message: String): SlackResponse
fun notifyError(channel: String? = null, title: String, error: Throwable): SlackResponse
fun notifyWarning(channel: String? = null, title: String, message: String): SlackResponse
fun notifyInfo(channel: String? = null, title: String, message: String): SlackResponse
```

| 메서드 | 색상 | 용도 |
|--------|------|------|
| `notifySuccess` | 녹색 (#36a64f) | 성공 알림 |
| `notifyError` | 빨간색 (#ff0000) | 에러 알림 (스택트레이스 포함) |
| `notifyWarning` | 주황색 (#ff9800) | 경고 알림 |
| `notifyInfo` | 파란색 (#439FE0) | 정보 알림 |

---

### SlackMessageBuilder (DSL)

#### 진입점

```kotlin
fun slackMessage(block: SlackMessageBuilder.() -> Unit): SlackMessage
```

#### 기본 메서드

| 메서드 | 설명 |
|--------|------|
| `channel(channel: String)` | 채널 설정 |
| `text(text: String)` | fallback 텍스트 (알림 미리보기용) |
| `threadTs(ts: String)` | 스레드 답장 설정 |

#### Block Kit 블록

| 메서드 | 설명 |
|--------|------|
| `header(text: String)` | 헤더 블록 (150자 제한) |
| `section { }` | 섹션 블록 |
| `divider()` | 구분선 |
| `actions { }` | 버튼 등 액션 블록 |
| `context { }` | 부가 정보 블록 |

#### Attachment (색상 바)

```kotlin
fun attachment(color: SlackColor = SlackColor.DEFAULT, block: AttachmentBuilder.() -> Unit)
fun attachment(hexColor: String, block: AttachmentBuilder.() -> Unit)
```

Block Kit은 색상 바를 지원하지 않습니다. 색상 바가 필요하면 Attachment를 사용하세요.

---

### SectionBlockBuilder

```kotlin
section {
    markdown("*Bold* text")           // 마크다운 텍스트
    text("Plain text")                // 일반 텍스트
    fields("*Label1*", "Value1",      // 2열 필드 (짝수 개)
           "*Label2*", "Value2")
}
```

| 메서드 | 설명 |
|--------|------|
| `markdown(text: String)` | mrkdwn 형식 텍스트 |
| `text(text: String)` | plain_text 형식 텍스트 |
| `fields(vararg texts: String)` | 2열 필드 (라벨-값 쌍) |

---

### ActionsBlockBuilder

```kotlin
actions {
    button("기본 버튼") { actionId("btn_default") }
    button("강조 버튼", ButtonStyle.PRIMARY) { url("https://...") }
    button("위험 버튼", ButtonStyle.DANGER) { actionId("btn_danger"); value("data") }
}
```

#### button

```kotlin
fun button(text: String, style: ButtonStyle = ButtonStyle.DEFAULT, block: ButtonBuilder.() -> Unit = {})
```

#### ButtonBuilder

| 메서드 | 설명 |
|--------|------|
| `actionId(id: String)` | 콜백용 ID |
| `value(value: String)` | 콜백용 값 |
| `url(url: String)` | 외부 링크 (클릭 시 이동) |

#### ButtonStyle

| 값 | 설명 |
|----|------|
| `DEFAULT` | 기본 (회색) |
| `PRIMARY` | 강조 (녹색) |
| `DANGER` | 위험 (빨간색) |

---

### ContextBlockBuilder

```kotlin
context {
    markdown("_작성자: Bot_")
    text("Plain text")
}
```

| 메서드 | 설명 |
|--------|------|
| `markdown(text: String)` | mrkdwn 형식 |
| `text(text: String)` | plain_text 형식 |

---

### AttachmentBuilder

Attachment 내부에 Block Kit 블록을 추가할 수 있습니다.

```kotlin
attachment(SlackColor.SUCCESS) {
    header("제목")
    section { markdown("내용") }
    divider()
    actions { button("버튼") { } }
    context { markdown("푸터") }
}
```

---

### SlackColor

| 값 | 색상 코드 | 용도 |
|----|----------|------|
| `DEFAULT` | #dddddd | 기본 (회색) |
| `SUCCESS` | #36a64f | 성공 (녹색) |
| `WARNING` | #ff9800 | 경고 (주황색) |
| `DANGER` | #ff0000 | 위험 (빨간색) |
| `INFO` | #439FE0 | 정보 (파란색) |

HEX 코드로 커스텀 색상을 지정할 수 있습니다:

```kotlin
attachment("#7B68EE") { ... }
```

---

### SlackResponse

```kotlin
data class SlackResponse(
    val ts: String,       // 메시지 타임스탬프 (스레드 답장 시 사용)
    val channel: String,  // 채널 ID
)
```

`SlackResponse.DISABLED`: Slack이 비활성화되면 반환되는 빈 응답

---

## 사용 예시

### 배포 알림

```kotlin
slackNotificationService.notify("#deployments", slackMessage {
    header("배포 완료")
    section {
        markdown("*앱:* `user-service`\n*버전:* `v2.3.1`\n*환경:* Production")
    }
    divider()
    attachment(SlackColor.SUCCESS) {
        section {
            fields("*Status*", "Success", "*Duration*", "3m 24s")
        }
    }
    actions {
        button("대시보드", ButtonStyle.PRIMARY) { url("https://dashboard.example.com") }
        button("롤백", ButtonStyle.DANGER) { actionId("rollback"); value("v2.3.1") }
    }
})
```

### 에러 알림 (try-catch)

```kotlin
try {
    processOrder(orderId)
} catch (e: Exception) {
    slackNotificationService.notifyError(
        channel = "#errors",
        title = "주문 처리 실패",
        error = e,
    )
    throw e
}
```

### 스레드 답장

```kotlin
val response = slackNotificationService.notify("#deployments", slackMessage {
    header("배포 시작")
    section { markdown("*앱:* `user-service`") }
})

// 진행 상황을 스레드에 답장
slackNotificationService.reply("#deployments", response.ts, slackMessage {
    text("빌드 완료 (1/3)")
})

slackNotificationService.reply("#deployments", response.ts, slackMessage {
    text("테스트 통과 (2/3)")
})

slackNotificationService.reply("#deployments", response.ts, slackMessage {
    attachment(SlackColor.SUCCESS) {
        section { markdown("*배포 완료* (3/3)") }
    }
})
```

### 주간 리포트 (스케줄러)

```kotlin
@Scheduled(cron = "0 0 9 * * MON")
fun sendWeeklyReport() {
    val stats = reportService.getWeeklyStats()

    slackNotificationService.notify("#reports", slackMessage {
        header("주간 리포트")
        section {
            fields(
                "*총 주문*", "${stats.totalOrders}건",
                "*총 매출*", "${stats.totalRevenue}원",
                "*신규 가입*", "${stats.newUsers}명",
                "*활성 사용자*", "${stats.activeUsers}명",
            )
        }
        context {
            markdown("기간: ${stats.startDate} ~ ${stats.endDate}")
        }
    })
}
```

---

## 예외 처리

### SlackException

```kotlin
class SlackException(
    val slackError: String,  // Slack API 에러 코드
    message: String,
    cause: Throwable? = null,
) : BizRuntimeException(ErrorCode.CALL_RESPONSE_ERROR, message, cause)
```

### 팩토리 메서드

| 메서드 | 에러 코드 | 설명 |
|--------|----------|------|
| `SlackException.of(error, warning)` | (동적) | Slack API 응답에서 생성 |
| `SlackException.channelNotFound(channel)` | `channel_not_found` | 채널을 찾을 수 없음 |
| `SlackException.notInChannel(channel)` | `not_in_channel` | 봇이 채널에 초대되지 않음 |
| `SlackException.invalidAuth()` | `invalid_auth` | 토큰 인증 실패 |

### Rate Limit 처리

Rate limit이 발생하면 `Retry-After` 헤더를 읽어 자동으로 재시도합니다. Coroutine `delay`를 사용하므로 스레드를 블로킹하지 않습니다.

---

## 자동 Footer

모든 메시지에 자동으로 footer가 추가됩니다.

```
_{appName}:{activeProfile} | x-b3-traceid: {traceId}_
```

- `appName`: `spring.application.name`
- `activeProfile`: `spring.profiles.active`
- `traceId`: MDC의 `traceId` 키

---

## Slack 설정 가이드

### Bot Token 발급

1. [Slack API](https://api.slack.com/apps) 접속
2. **Create New App** → **From scratch**
3. App Name 입력, Workspace 선택
4. **OAuth & Permissions** 메뉴
5. **Bot Token Scopes** 추가:
   - `chat:write` (메시지 전송)
   - `chat:write.public` (봇이 초대되지 않은 public 채널에도 전송 가능)
6. **Install to Workspace**
7. **Bot User OAuth Token** 복사 (`xoxb-...`)

### 환경 변수 설정

```bash
export SLACK_BOT_TOKEN=xoxb-your-token-here
```

### 채널에 봇 초대

```
/invite @your-bot-name
```

---

## 모듈 구조

```
modules/infrastructure/src/main/kotlin/com/myrealtrip/infrastructure/slack/
├── SlackClient.kt              # SDK 래퍼 (재시도 로직 포함)
├── SlackClientConfig.kt        # Bean 설정
├── SlackException.kt           # 커스텀 예외
├── SlackNotificationService.kt # 고수준 알림 서비스
├── SlackProperties.kt          # 설정 프로퍼티
├── SlackResponse.kt            # 응답 모델
└── message/
    ├── ActionsBlockBuilder.kt  # 버튼 블록 빌더
    ├── AttachmentBuilder.kt    # Attachment 빌더
    ├── ButtonStyle.kt          # 버튼 스타일 enum
    ├── ContextBlockBuilder.kt  # Context 블록 빌더
    ├── SectionBlockBuilder.kt  # Section 블록 빌더
    ├── SlackColor.kt           # 색상 enum
    ├── SlackDsl.kt             # @DslMarker
    ├── SlackMessage.kt         # 메시지 도메인 모델
    └── SlackMessageBuilder.kt  # DSL 빌더
```
