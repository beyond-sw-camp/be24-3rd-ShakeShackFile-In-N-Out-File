package com.example.WaffleBear.order.model.dto;

import java.math.BigDecimal;

public class OrderDto {

    public record OrderRequest(
            String planType,
            BigDecimal amount
    ) {}

    public record OrderResponse(
            String orderId,
            String planType,
            BigDecimal amount
    ) {}

    public record OrderVerifyRequest(
            String paymentId,
            String orderId
    ) {}
}
