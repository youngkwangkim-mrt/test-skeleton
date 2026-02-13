package com.myrealtrip.domain.holiday.entity

import com.myrealtrip.domain.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "holidays",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_holidays_01", columnNames = ["holiday_date", "name"])
    ]
)
class Holiday(
    holidayDate: LocalDate,
    name: String,
    id: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = id

    @Column(name = "holiday_date", nullable = false)
    var holidayDate: LocalDate = holidayDate
        private set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        private set

    fun update(holidayDate: LocalDate, name: String) {
        this.holidayDate = holidayDate
        this.name = name
    }

    companion object {
        fun create(holidayDate: LocalDate, name: String) = Holiday(
            holidayDate = holidayDate,
            name = name,
        )
    }
}
