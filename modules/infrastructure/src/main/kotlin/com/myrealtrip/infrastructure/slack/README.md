# Slack Notification

Kotlin DSL로 Slack 알림을 전송합니다. Block Kit을 기본으로 사용하며, 색상 바가 필요한 경우에만 Attachment를 사용합니다.

---

## Quick Start

```kotlin
// 간단한 메시지
slackNotificationService.notify("#alerts", slackMessage {
    text("배포가 완료되었습니다!")
})

// Block Kit 메시지
slackNotificationService.notify("#deployments", slackMessage {
    header("배포 알림")
    section { markdown("*앱:* `user-service`\n*버전:* `v2.3.1`") }
    divider()
    actions {
        button("로그 보기", ButtonStyle.PRIMARY) { url("https://logs.example.com") }
    }
})

// 편의 메서드
slackNotificationService.notifySuccess("#alerts", "배포 완료", "user-service v2.3.1")
slackNotificationService.notifyError("#alerts", "배포 실패", exception)
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

### Bot Token 발급

1. [Slack API](https://api.slack.com/apps) 접속
2. **Create New App** → **From scratch**
3. **OAuth & Permissions** 메뉴에서 Bot Token Scopes 추가:
   - `chat:write` (메시지 전송)
   - `chat:write.public` (봇이 초대되지 않은 public 채널에도 전송 가능)
4. **Install to Workspace**
5. **Bot User OAuth Token** 복사 (`xoxb-...`)

---

## Block Kit 사용법 (권장)

Block Kit은 Slack의 현대적인 메시지 포맷입니다. 특별한 이유가 없다면 Block Kit을 사용하세요.

### 기본 구조

```kotlin
slackNotificationService.notify("#channel", slackMessage {
    // 1. 헤더 (선택)
    header("제목")

    // 2. 본문
    section { markdown("*굵은 글씨* 일반 글씨") }

    // 3. 구분선 (선택)
    divider()

    // 4. 필드 형식 (2열 레이아웃)
    section {
        fields(
            "*라벨1*", "값1",
            "*라벨2*", "값2",
        )
    }

    // 5. 버튼 (선택)
    actions {
        button("버튼 텍스트", ButtonStyle.PRIMARY) { url("https://...") }
    }

    // 6. 부가 정보 (선택)
    context { markdown("_작성자: Bot_") }
})
```

### Header 블록

메시지 상단에 큰 텍스트로 표시됩니다. 150자 제한.

```kotlin
slackMessage {
    header("배포 알림")
}
```

### Section 블록

본문 텍스트를 표시합니다. `markdown()` 또는 `text()`를 사용합니다.

```kotlin
slackMessage {
    // 마크다운 텍스트
    section { markdown("*굵게* _기울임_ `코드`") }

    // 일반 텍스트
    section { text("Plain text") }

    // 2열 필드 (라벨-값 쌍, 짝수 개)
    section {
        fields(
            "*환경*", "Production",
            "*버전*", "v2.3.1",
            "*상태*", "Success",
            "*소요시간*", "3분 24초",
        )
    }
}
```

### Divider 블록

구분선을 추가합니다.

```kotlin
slackMessage {
    section { markdown("위쪽 내용") }
    divider()
    section { markdown("아래쪽 내용") }
}
```

### Actions 블록

버튼을 추가합니다.

```kotlin
slackMessage {
    actions {
        // 기본 버튼
        button("기본") { actionId("btn_default") }

        // 강조 버튼 (녹색)
        button("강조", ButtonStyle.PRIMARY) { url("https://...") }

        // 위험 버튼 (빨간색)
        button("삭제", ButtonStyle.DANGER) {
            actionId("btn_delete")
            value("item_123")
        }
    }
}
```

**ButtonStyle:**

| 스타일 | 설명 |
|--------|------|
| `DEFAULT` | 기본 (회색) |
| `PRIMARY` | 강조 (녹색) |
| `DANGER` | 위험 (빨간색) |

**ButtonBuilder:**

| 메서드 | 설명 |
|--------|------|
| `actionId(id)` | 콜백용 ID |
| `value(value)` | 콜백용 값 |
| `url(url)` | 외부 링크 (클릭 시 이동) |

### Context 블록

작은 글씨로 부가 정보를 표시합니다.

```kotlin
slackMessage {
    context {
        markdown("_작성자: @john.doe | 2024-01-15 14:30_")
    }
}
```

---

## 색상 바 사용법 (Attachment)

> **주의**: Attachment는 Slack의 **레거시 포맷**입니다. Block Kit은 색상 바를 지원하지 않으므로, 색상 바가 꼭 필요한 경우에만 Attachment를 사용하세요. 새로운 기능 개발 시에는 Block Kit 사용을 권장합니다.

### 기본 사용법

```kotlin
slackNotificationService.notify("#alerts", slackMessage {
    attachment(SlackColor.SUCCESS) {
        section { markdown("*배포 완료*") }
    }
})
```

### SlackColor

| 색상 | 코드 | 용도 |
|------|------|------|
| `DEFAULT` | #dddddd | 기본 (회색) |
| `SUCCESS` | #36a64f | 성공 (녹색) |
| `WARNING` | #ff9800 | 경고 (주황색) |
| `DANGER` | #ff0000 | 위험 (빨간색) |
| `INFO` | #439FE0 | 정보 (파란색) |

### 커스텀 색상

HEX 코드로 커스텀 색상을 지정할 수 있습니다:

```kotlin
slackMessage {
    attachment("#7B68EE") {  // 보라색
        section { markdown("커스텀 색상") }
    }
}
```

### Attachment 내부에서 Block Kit 사용

Attachment 내부에 Block Kit 블록을 추가할 수 있습니다:

```kotlin
slackMessage {
    attachment(SlackColor.SUCCESS) {
        header("배포 완료")
        section { markdown("*앱:* `user-service`") }
        divider()
        section {
            fields("*상태*", "Success", "*소요시간*", "3분 24초")
        }
        context { markdown("_배포자: @john.doe_") }
    }
}
```

---

## 편의 메서드

자주 사용하는 알림 유형을 위한 편의 메서드입니다. 내부적으로 색상 바를 표시하기 위해 Attachment(레거시)를 사용합니다.

```kotlin
// 성공 알림 (녹색)
slackNotificationService.notifySuccess("#alerts", "배포 완료", "user-service v2.3.1")

// 에러 알림 (빨간색, 스택트레이스 포함)
slackNotificationService.notifyError("#errors", "주문 처리 실패", exception)

// 경고 알림 (주황색)
slackNotificationService.notifyWarning("#alerts", "디스크 경고", "사용량 85%")

// 정보 알림 (파란색)
slackNotificationService.notifyInfo("#info", "공지", "시스템 점검 예정")
```

| 메서드 | 색상 | 용도 |
|--------|------|------|
| `notifySuccess` | 녹색 (#36a64f) | 성공 알림 |
| `notifyError` | 빨간색 (#ff0000) | 에러 알림 (스택트레이스 포함) |
| `notifyWarning` | 주황색 (#ff9800) | 경고 알림 |
| `notifyInfo` | 파란색 (#439FE0) | 정보 알림 |

---

## 스레드 답장

원본 메시지에 스레드로 답장합니다. 배포 진행 상황 등에 유용합니다.

```kotlin
// 1. 원본 메시지 전송
val response = slackNotificationService.notify("#deployments", slackMessage {
    header("배포 시작")
    section { markdown("*앱:* `user-service`") }
})

// 2. 스레드에 답장 (response.ts 사용)
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
    section {
        fields("*상태*", "Success", "*소요시간*", "3분 24초")
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

### 시스템 모니터링

```kotlin
fun checkDiskUsage() {
    val usage = systemService.getDiskUsage()

    if (usage > 90) {
        slackNotificationService.notifyError("#alerts", "디스크 위험", "사용량 ${usage}%")
    } else if (usage > 80) {
        slackNotificationService.notifyWarning("#alerts", "디스크 경고", "사용량 ${usage}%")
    }
}
```

---

## 비활성화

테스트 환경이나 로컬에서 Slack 전송을 비활성화하려면:

```yaml
slack:
  enabled: false
```

비활성화하면 `SlackResponse.DISABLED`를 반환하고 실제 전송은 하지 않습니다.

---

## 에러 처리

### SlackException

Slack API 호출 실패 시 `SlackException`이 발생합니다.

| 에러 코드 | 설명 | 해결 방법 |
|----------|------|----------|
| `channel_not_found` | 채널을 찾을 수 없음 | 채널명 확인 |
| `not_in_channel` | 봇이 채널에 없음 | `/invite @bot-name` 으로 초대 |
| `invalid_auth` | 토큰 인증 실패 | Bot Token 확인 |
| `rate_limited` | Rate limit 초과 | 자동 재시도됨 |

### Rate Limit 처리

Rate limit이 발생하면 `Retry-After` 헤더를 읽어 자동으로 재시도합니다. Coroutine `delay`를 사용하므로 스레드를 블로킹하지 않습니다.

---

## 자동 Footer

모든 메시지에 자동으로 footer가 추가됩니다:

```
_{appName}:{activeProfile} | x-b3-traceid: {traceId}_
```

- `appName`: `spring.application.name`
- `activeProfile`: `spring.profiles.active`
- `traceId`: MDC의 `traceId` 키

---

## 파일 구조

```
slack/
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
