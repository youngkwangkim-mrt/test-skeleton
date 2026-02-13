package com.myrealtrip.domain.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity : BaseTimeEntity() {

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    lateinit var createdBy: String
        protected set

    @LastModifiedBy
    @Column(name = "modified_by", nullable = false, length = 50)
    lateinit var modifiedBy: String
        protected set
}
