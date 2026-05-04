package com.tenslots.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false, columnDefinition = "DATETIME")
    @Comment("생성일시 (UTC)")
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(updatable = false, nullable = false, length = 50)
    @Comment("생성자")
    private String createdBy;

    @LastModifiedDate
    @Column(nullable = false, columnDefinition = "DATETIME")
    @Comment("수정일시 (UTC)")
    private OffsetDateTime updatedAt;

    @LastModifiedBy
    @Column(nullable = false, length = 50)
    @Comment("수정자")
    private String updatedBy;
}
