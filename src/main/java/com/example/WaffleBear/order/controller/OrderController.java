package com.example.WaffleBear.order.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.order.model.dto.OrderDto;
import com.example.WaffleBear.order.service.OrderService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제 (Order)", description = "PortOne 결제 주문 생성 및 검증 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "상품 코드를 기반으로 새로운 결제 주문을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상품 코드"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping
    public BaseResponse<OrderDto.OrderResponse> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "주문 생성 요청 정보") @RequestBody OrderDto.OrderRequest requestDto) {
        OrderDto.OrderResponse responseDto = orderService.createOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success(responseDto);
    }

    @Operation(summary = "결제 검증 및 완료", description = "PortOne 결제 ID와 주문 ID를 검증하고 결제를 완료 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 검증 및 완료 성공"),
            @ApiResponse(responseCode = "400", description = "결제 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PostMapping("/verify")
    public BaseResponse<String> verifyAndCompleteOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails userDetails,
            @Parameter(description = "결제 검증 요청 정보") @RequestBody OrderDto.OrderVerifyRequest requestDto) {
        orderService.verifyAndCompleteOrder(userDetails.getEmail(), requestDto);
        return BaseResponse.success("결제가 정상적으로 완료되었습니다.");
    }
}