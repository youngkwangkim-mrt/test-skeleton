package com.myrealtrip.infrastructure.slack.message

/**
 * Slack DSL 마커 어노테이션
 *
 * DSL 빌더 스코프를 제한하여 잘못된 중첩 호출을 방지합니다.
 */
@DslMarker
annotation class SlackDsl
