package com.myrealtrip.infrastructure.slack

import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** 메시지 전송 시점에 appName, activeProfile, traceId를 포함한 footer context block을 생성합니다. */
@Component
class SlackFooterProvider(
    @param:Value("\${spring.application.name:unknown}")
    private val appName: String,
    @param:Value("\${spring.profiles.active:unknown}")
    private val activeProfile: String,
) {

    fun buildFooterBlock(): LayoutBlock {
        val traceId = MDC.get("traceId") ?: "N/A"
        return ContextBlock.builder()
            .elements(
                listOf(
                    MarkdownTextObject.builder()
                        .text("_${appName}:${activeProfile} | x-b3-traceid: $traceId _")
                        .build(),
                ),
            )
            .build()
    }
}
