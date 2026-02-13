package com.myrealtrip.domain.notification

import com.myrealtrip.common.notification.NotificationEvent
import com.myrealtrip.common.notification.message.MessageColor
import com.myrealtrip.common.notification.message.slackMessage
import java.time.LocalDate

object HolidayNotificationEventFactory {

    fun holidayCreated(
        id: Long,
        holidayDate: LocalDate,
        name: String,
    ): NotificationEvent.Slack = NotificationEvent.Slack(
        message = slackMessage {
            attachment(MessageColor.SUCCESS) {
                section { markdown("*공휴일이 등록되었습니다*") }
                section {
                    fields(
                        "*ID*", "$id",
                        "*날짜*", "$holidayDate",
                        "*이름*", name,
                    )
                }
            }
        }
    )

    fun holidayUpdated(
        id: Long,
        holidayDate: LocalDate,
        name: String,
    ): NotificationEvent.Slack = NotificationEvent.Slack(
        message = slackMessage {
            attachment(MessageColor.SUCCESS) {
                section { markdown("*공휴일이 수정되었습니다*") }
                section {
                    fields(
                        "*ID*", "$id",
                        "*날짜*", "$holidayDate",
                        "*이름*", name,
                    )
                }
            }
        }
    )

    fun holidayDeleted(
        id: Long,
    ): NotificationEvent.Slack = NotificationEvent.Slack(
        message = slackMessage {
            attachment(MessageColor.WARNING) {
                section { markdown("*공휴일이 삭제되었습니다*") }
                section {
                    fields("*ID*", "$id")
                }
            }
        }
    )

    fun holidayBulkCreated(
        count: Int,
    ): NotificationEvent.Slack = NotificationEvent.Slack(
        message = slackMessage {
            attachment(MessageColor.SUCCESS) {
                section { markdown("*공휴일이 일괄 등록되었습니다*") }
                section {
                    fields("*등록 건수*", "${count}건")
                }
            }
        }
    )
}