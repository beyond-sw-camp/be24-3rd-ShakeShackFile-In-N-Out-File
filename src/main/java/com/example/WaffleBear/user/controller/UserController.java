package com.example.WaffleBear.user.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.UserDto;
import com.example.WaffleBear.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Signup and email verification APIs")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up", description = "Creates a new local account and starts the email verification flow.")
    public ResponseEntity<?> signup(@RequestBody UserDto.SignupReq dto) {
        UserDto.SignupRes result = userService.signup(dto);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email", description = "Completes email verification with the issued token.")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("성공");
    }
}
