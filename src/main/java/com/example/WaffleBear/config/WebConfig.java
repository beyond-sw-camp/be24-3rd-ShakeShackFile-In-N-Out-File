package com.example.WaffleBear.config;


import com.example.WaffleBear.config.interceptor.CheckRoomAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final CheckRoomAuthInterceptor checkRoomAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 채팅방 상세 페이지나 관련 API 호출 시 권한 체크
        // 현재는 /chat/room/{id} 경로에 대해 적용
        registry.addInterceptor(checkRoomAuthInterceptor)
                .addPathPatterns("/chat/{roomidx}/**");
    }
}
