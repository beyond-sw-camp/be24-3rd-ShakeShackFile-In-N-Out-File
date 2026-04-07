package com.example.WaffleBear.order.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.order.model.dto.OrderDto;
import com.example.WaffleBear.order.service.OrderService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Plan purchase and payment verification APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order", description = "Creates an order draft for a plan or product.")
    public BaseResponse<OrderDto.OrderResponse> createOrder(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody OrderDto.OrderRequest requestDto) {
        OrderDto.OrderResponse responseDto = orderService.createOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success(responseDto);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment", description = "Verifies the payment and finalizes the order.")
    public BaseResponse<String> verifyAndCompleteOrder(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody OrderDto.OrderVerifyRequest requestDto) {
        orderService.verifyAndCompleteOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success("결제가 정상적으로 완료되었습니다.");
    }
}
