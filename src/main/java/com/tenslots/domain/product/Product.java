package com.tenslots.domain.product;

import com.tenslots.domain.common.BaseEntity;
import com.tenslots.domain.common.DomainConstants;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Comment("상품")
@SQLRestriction("deleted_at is null")
@SQLDelete(sql = "update product set deleted_at = now() where id = ?")
@Table(indexes = {
        @Index(name = "idx_product_open_at", columnList = "open_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @Tsid
    @Column(length = DomainConstants.TSID_LENGTH)
    @Comment("상품 ID (TSID)")
    private String id;

    @Column(nullable = false, length = DomainConstants.VARCHAR_LENGTH)
    @Comment("상품명")
    private String name;

    @Column(nullable = false, precision = DomainConstants.PRICE_PRECISION, scale = DomainConstants.PRICE_SCALE)
    @Comment("가격")
    private BigDecimal price;

    @Column(nullable = false, columnDefinition = "DATETIME")
    @Comment("체크인 시간")
    private OffsetDateTime checkInTime;

    @Column(nullable = false, columnDefinition = "DATETIME")
    @Comment("체크아웃 시간")
    private OffsetDateTime checkOutTime;

    @Column(nullable = false, columnDefinition = "DATETIME")
    @Comment("판매 오픈 시각")
    private OffsetDateTime openAt;

    @Builder
    private Product(String name, BigDecimal price, OffsetDateTime checkInTime, OffsetDateTime checkOutTime, OffsetDateTime openAt) {
        this.name = name;
        this.price = price;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.openAt = openAt;
    }
}
