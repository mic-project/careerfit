package com.codelab.micproject.booking.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum SessionBundle {
    ONE(1, 0.00),
    THREE(3, 0.21),
    FIVE(5, 0.32);

    private final int count;
    private final double discountRate;

    SessionBundle(int count, double discountRate) {
        this.count = count;
        this.discountRate = discountRate;
    }

    public BigDecimal totalPrice(BigDecimal unit) {
        var subtotal = unit.multiply(BigDecimal.valueOf(count));
        var discount = subtotal.multiply(BigDecimal.valueOf(discountRate));
        return subtotal.subtract(discount).setScale(0, java.math.RoundingMode.HALF_UP);
    }
}