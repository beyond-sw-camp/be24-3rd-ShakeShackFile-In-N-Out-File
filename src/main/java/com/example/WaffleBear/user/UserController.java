package com.example.WaffleBear.user;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity signup(@RequestBody UserDto.SignupReq dto){
        UserDto.SignupRes result = userService.signup(dto);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        userService.verifyEmail(token);
        return ResponseEntity.ok("성공");
    }
}
