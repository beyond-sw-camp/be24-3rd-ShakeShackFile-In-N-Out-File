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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentClient portoneClient;

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

        try {
            // portone API V2 verification
            var paymentResponse = portoneClient.getPayment(requestDto.paymentId());
            
            if (paymentResponse == null) {
                throw new BaseException(BaseResponseStatus.FAIL);
            }

            order.setPaymentId(requestDto.paymentId());
            orderRepository.save(order);

            User user = order.getUser();
            user.setPlanType(order.getPlanType());
            user.setPaymentId(requestDto.paymentId());
            userRepository.save(user);

        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.FAIL);
        }
    }
}
