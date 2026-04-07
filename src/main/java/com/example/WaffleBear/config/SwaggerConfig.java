package com.example.WaffleBear.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {

    @Value("${app.backend-url:http://localhost/api}")
    private String backendUrl;

    @Bean
    public OpenAPI openAPI() {
        ObjectSchema loginRequestSchema = new ObjectSchema();
        loginRequestSchema.addProperty("email", new StringSchema().example("user@example.com"));
        loginRequestSchema.addProperty("password", new StringSchema().example("P@ssw0rd!"));
        loginRequestSchema.addProperty("name", new StringSchema().example("optional"));
        loginRequestSchema.setRequired(List.of("email", "password"));

        ObjectSchema loginResponseSchema = new ObjectSchema();
        loginResponseSchema.addProperty("accessToken", new StringSchema());
        loginResponseSchema.addProperty("email", new StringSchema());
        loginResponseSchema.addProperty("role", new StringSchema().example("ROLE_USER"));

        Operation loginOperation = new Operation()
                .addTagsItem("Auth")
                .summary("Login")
                .description("Authenticates with email and password, returns an access token in the response body and Authorization header, and issues a refresh cookie.")
                .requestBody(new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType().schema(loginRequestSchema)
                        )))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("Login succeeded")
                                .content(new Content().addMediaType(
                                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                        new MediaType().schema(loginResponseSchema)
                                )))
                        .addApiResponse("401", new ApiResponse().description("Invalid credentials")));

        return new OpenAPI()
                .info(new Info()
                        .title("WaffleBear API")
                        .description("OpenAPI documentation for the WaffleBear backend features.")
                        .version("1.0.0"))
                .servers(List.of(new Server().url(backendUrl).description("Configured backend server")))
                .path("/login", new PathItem().post(loginOperation));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/login", "/user/**", "/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/administrator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("drive")
                .pathsToMatch("/file/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workspaceApi() {
        return GroupedOpenApi.builder()
                .group("workspace")
                .pathsToMatch("/workspace/**")
                .build();
    }

    @Bean
    public GroupedOpenApi groupApi() {
        return GroupedOpenApi.builder()
                .group("group")
                .pathsToMatch("/group/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("chat")
                .pathsToMatch("/chat/**", "/chatRoom/**", "/sse/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification")
                .pathsToMatch("/notification/**")
                .build();
    }

    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("order")
                .pathsToMatch("/orders/**")
                .build();
    }

    @Bean
    public GroupedOpenApi gameApi() {
        return GroupedOpenApi.builder()
                .group("game")
                .pathsToMatch("/game/**")
                .build();
    }
}
