package com.example.WaffleBear.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class SwaggerConfig {

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 스키마
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Access Token을 입력하세요. (Bearer 접두사 자동 추가)");

        // 로그인 요청 스키마
        Schema<?> loginRequestSchema = new Schema<>()
                .type("object")
                .addProperty("email", new Schema<>().type("string").example("user@example.com").description("이메일 주소"))
                .addProperty("password", new Schema<>().type("string").example("Qwer1234%").description("비밀번호"))
                .required(List.of("email", "password"));

        // 로그인 응답 스키마
        Schema<?> loginResponseSchema = new Schema<>()
                .type("object")
                .addProperty("accessToken", new Schema<>().type("string").description("JWT Access Token"))
                .addProperty("email", new Schema<>().type("string").description("사용자 이메일"))
                .addProperty("role", new Schema<>().type("string").description("사용자 권한 (ROLE_USER / ROLE_ADMIN)"));

        // 로그인 API (LoginFilter가 처리하므로 수동 등록)
        Operation loginOperation = new Operation()
                .tags(List.of("인증 (Auth)"))
                .summary("로그인")
                .description("이메일과 비밀번호로 로그인합니다. 성공 시 Access Token(헤더)과 Refresh Token(쿠키)이 발급됩니다.\n\n"
                        + "- Access Token: 응답 본문 + Authorization 헤더\n"
                        + "- Refresh Token: HttpOnly 쿠키 (14일 유효)")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType("application/json",
                                new MediaType().schema(loginRequestSchema))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("로그인 성공")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(loginResponseSchema))))
                        .addApiResponse("401", new ApiResponse().description("이메일 또는 비밀번호가 일치하지 않습니다")));

        return new OpenAPI()
                .info(new Info()
                        .title("FileInNOut API")
                        .description("WaffleBear 파일 공유 플랫폼 REST API 문서\n\n"
                                + "### 주요 기능\n"
                                + "- 파일 업로드/다운로드 (MinIO/S3/로컬)\n"
                                + "- OAuth2 소셜 로그인 (Google/Naver/Kakao)\n"
                                + "- 실시간 채팅 (WebSocket/STOMP)\n"
                                + "- SSE 알림\n"
                                + "- 워크스페이스 협업\n"
                                + "- 결제 (PortOne)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WaffleBear Team")
                                .email("admin@wafflebear.com")))
                .servers(List.of(
                        new Server().url(backendUrl).description("현재 서버")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", jwtScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .path("/login", new PathItem().post(loginOperation));
    }
}
