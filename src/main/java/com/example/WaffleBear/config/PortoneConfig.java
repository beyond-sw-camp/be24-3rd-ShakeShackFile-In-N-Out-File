package com.example.WaffleBear.config;

import io.portone.sdk.server.payment.PaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortoneConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${portone.portone_secret:}')")
    public PaymentClient iamportClient(@Value("${portone.portone_secret:}") String apiSecret) {
        return new PaymentClient(apiSecret, "https://api.portone.io", null);
    }
}
