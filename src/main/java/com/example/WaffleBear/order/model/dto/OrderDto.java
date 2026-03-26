package com.example.WaffleBear.order.model.dto;

import java.math.BigDecimal;

public class OrderDto {

    public record OrderRequest(
            String productCode
    ) {}

    public record OrderResponse(
            String orderId,
            String productCode,
            String productName,
            BigDecimal amount
    ) {}

    public record OrderVerifyRequest(
            String paymentId,
            String orderId
    ) {}
}