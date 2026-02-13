package com.myrealtrip.domain.notification

sealed interface DomainEvent {

    sealed class Reservation(val reservationId: Long) : DomainEvent {
        data class Created(val id: Long) : Reservation(id)
        data class Confirmed(val id: Long) : Reservation(id)
        data class RequestChange(val id: Long) : Reservation(id)
        data class Cancelled(val id: Long) : Reservation(id)
        data class Completed(val id: Long) : Reservation(id)
    }
}