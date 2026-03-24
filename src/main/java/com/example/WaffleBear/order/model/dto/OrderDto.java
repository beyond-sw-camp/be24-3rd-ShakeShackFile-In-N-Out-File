package com.example.WaffleBear.order.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public class OrderDto {

    @Schema(description = "주문 생성 요청")
    public record OrderRequest(
            @Schema(description = "상품 코드", example = "PLAN_PRO")
            String productCode
    ) {}

    @Schema(description = "주문 생성 응답")
    public record OrderResponse(
            @Schema(description = "주문 ID", example = "ORD-20260324-001")
            String orderId,
            @Schema(description = "상품 코드", example = "PLAN_PRO")
            String productCode,
            @Schema(description = "상품명", example = "프로 플랜")
            String productName,
            @Schema(description = "결제 금액", example = "9900")
            BigDecimal amount
    ) {}

    @Schema(description = "결제 검증 요청")
    public record OrderVerifyRequest(
            @Schema(description = "PortOne 결제 ID", example = "pay_abc123")
            String paymentId,
            @Schema(description = "주문 ID", example = "ORD-20260324-001")
            String orderId
    ) {}
}
