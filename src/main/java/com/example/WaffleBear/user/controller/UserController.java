package com.example.WaffleBear.user.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.UserDto;
import com.example.WaffleBear.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 (User)", description = "회원가입, 이메일 인증 등 사용자 계정 관련 API")
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "회원가입", description = "이메일, 이름, 비밀번호로 새 계정을 생성합니다. 가입 후 이메일 인증이 필요할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 중복 또는 잘못된 요청 데이터")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto.SignupReq dto) {
        UserDto.SignupRes result = userService.signup(dto);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @Operation(summary = "이메일 인증", description = "이메일로 전송된 인증 토큰을 검증하여 계정을 활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 토큰")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(
            @Parameter(description = "이메일 인증 토큰", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("성공");
    }
}
