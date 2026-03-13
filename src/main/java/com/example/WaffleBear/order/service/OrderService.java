package com.example.WaffleBear.order.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.order.model.Order;
import com.example.WaffleBear.order.model.dto.OrderDto;
import com.example.WaffleBear.order.repository.OrderRepository;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<PaymentClient> paymentClientProvider;

    @Transactional
    public OrderDto.OrderResponse createOrder(String email, OrderDto.OrderRequest requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.FAIL));

        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .orderId(orderId)
                .user(user)
                .planType(requestDto.planType())
                .amount(requestDto.amount())
                .build();

        orderRepository.save(order);

        return new OrderDto.OrderResponse(
                order.getOrderId(),
                order.getPlanType(),
                order.getAmount()
        );
    }

    @Transactional
    public void verifyAndCompleteOrder(String email, OrderDto.OrderVerifyRequest requestDto) {
        Order order = orderRepository.findByOrderId(requestDto.orderId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.FAIL));

        User user = order.getUser();
        if (user == null || user.getEmail() == null || !user.getEmail().equals(email)) {
            throw new BaseException(BaseResponseStatus.FAIL);
        }

        try {
            PaymentClient portoneClient = paymentClientProvider.getIfAvailable();
            if (portoneClient == null) {
                throw new BaseException(BaseResponseStatus.FAIL);
            }

            // portone API V2 verification
            var paymentResponse = portoneClient.getPayment(requestDto.paymentId());
            
            if (paymentResponse == null) {
                throw new BaseException(BaseResponseStatus.FAIL);
            }

            order.setPaymentId(requestDto.paymentId());
            orderRepository.save(order);

            user.setRole(resolveRoleForPlanType(order.getPlanType(), user.getRole()));
            userRepository.save(user);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.FAIL);
        }
    }

    private String resolveRoleForPlanType(String planType, String currentRole) {
        if ("ROLE_ADMIN".equalsIgnoreCase(currentRole)) {
            return currentRole;
        }

        if (planType == null || planType.isBlank()) {
            return currentRole == null || currentRole.isBlank() ? "ROLE_USER" : currentRole;
        }

        String normalizedPlanType = planType.trim().toUpperCase();

        return switch (normalizedPlanType) {
            case "STARTER", "FREE", "BASIC" -> "ROLE_USER";
            case "PROFESSIONAL", "PRO", "PLUS", "PREMIUM" -> "ROLE_PREMIUM";
            case "ENTERPRISE", "VIP" -> "ROLE_ENTERPRISE";
            default -> currentRole == null || currentRole.isBlank() ? "ROLE_USER" : currentRole;
        };
    }
}
