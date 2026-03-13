package com.example.WaffleBear.order.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.order.model.dto.OrderDto;
import com.example.WaffleBear.order.service.OrderService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public BaseResponse<OrderDto.OrderResponse> createOrder(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody OrderDto.OrderRequest requestDto) {
        OrderDto.OrderResponse responseDto = orderService.createOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success(responseDto);
    }

    @PostMapping("/verify")
    public BaseResponse<String> verifyAndCompleteOrder(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody OrderDto.OrderVerifyRequest requestDto) {
        orderService.verifyAndCompleteOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success("결제가 성공적으로 완료되었습니다.");
    }
}
