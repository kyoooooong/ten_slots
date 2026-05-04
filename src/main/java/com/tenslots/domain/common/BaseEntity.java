package com.tenslots.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// soft delete가 필요한 엔티티에 사용 — @SQLRestriction("deleted_at is null")과 함께 선언
@MappedSuperclass
@Getter
public abstract class BaseEntity extends BaseTimeEntity {

    @Column(columnDefinition = "DATETIME")
    @Comment("삭제일시 (UTC, null이면 정상)")
    private OffsetDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
