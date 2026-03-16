package com.example.WaffleBear.order.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.service.StoragePlanService;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<PaymentClient> paymentClientProvider;
    private final StoragePlanService storagePlanService;

    @Transactional
    public OrderDto.OrderResponse createOrder(String email, OrderDto.OrderRequest requestDto) {
        if (requestDto == null || requestDto.productCode() == null || requestDto.productCode().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.FAIL));

        StoragePlanService.ProductDefinition product;
        try {
            product = storagePlanService.requireProduct(requestDto.productCode());
        } catch (IllegalArgumentException exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        if (product.amount() == null || product.amount().signum() <= 0) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String orderId = UUID.randomUUID().toString();
        LocalDateTime purchasedAt = LocalDateTime.now();

        Order order = Order.builder()
                .orderId(orderId)
                .user(user)
                .planType(product.code())
                .productCode(product.code())
                .productName(product.displayName())
                .productCategory(product.category())
                .billingCycle(product.durationYears() > 0 ? "YEARLY" : "NONE")
                .quotaBytes(product.quotaBytes())
                .expiresAt(storagePlanService.calculateExpiry(product, purchasedAt))
                .amount(product.amount())
                .build();

        orderRepository.save(order);

        return new OrderDto.OrderResponse(
                order.getOrderId(),
                order.getProductCode(),
                order.getProductName(),
                order.getAmount()
        );
    }

    @Transactional
    public void verifyAndCompleteOrder(String email, OrderDto.OrderVerifyRequest requestDto) {
        if (requestDto == null || requestDto.orderId() == null || requestDto.orderId().isBlank() || requestDto.paymentId() == null || requestDto.paymentId().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

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

            var paymentResponse = portoneClient.getPayment(requestDto.paymentId());
            if (paymentResponse == null) {
                throw new BaseException(BaseResponseStatus.FAIL);
            }

            StoragePlanService.ProductDefinition product = storagePlanService.resolveProduct(order.getProductCode());

            order.setPaymentId(requestDto.paymentId());
            if (product != null) {
                order.setPlanType(product.code());
                order.setProductCode(product.code());
                order.setProductName(product.displayName());
                order.setProductCategory(product.category());
                order.setBillingCycle(product.durationYears() > 0 ? "YEARLY" : "NONE");
                order.setQuotaBytes(product.quotaBytes());
                order.setExpiresAt(storagePlanService.calculateExpiry(product, LocalDateTime.now()));
                if (order.getAmount() == null) {
                    order.setAmount(product.amount());
                }
            }

            orderRepository.save(order);
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BaseException(BaseResponseStatus.FAIL);
        }
    }
}