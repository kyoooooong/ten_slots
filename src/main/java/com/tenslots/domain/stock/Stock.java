package com.tenslots.domain.stock;

import com.tenslots.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Comment("재고")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @Column(length = 13)
    @Comment("상품 ID (product.id 참조)")
    private String productId;

    @Comment("잔여 재고 수량")
    private int quantity;

    @Comment("최대 재고 수량 (increase() 상한 체크에 사용)")
    private int maxQuantity;

    @Version
    @Comment("낙관적 락 버전")
    private Long version;

    @Builder
    private Stock(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.maxQuantity = quantity;
    }

    public boolean decrease() {
        if (this.quantity <= 0) {
            return false;
        }
        this.quantity--;
        return true;
    }

    public void increase() {
        if (this.quantity >= this.maxQuantity) {
            throw new StockOverflowException(this.productId, this.quantity, this.maxQuantity);
        }
        this.quantity++;
    }
}
