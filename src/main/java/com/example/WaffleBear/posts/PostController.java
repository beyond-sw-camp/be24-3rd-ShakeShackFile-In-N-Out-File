package com.example.WaffleBear.posts;

import com.example.WaffleBear.posts.model.PostDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.UserRepository;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//
//@RequestMapping("/editor")
//@RequiredArgsConstructor
//@RestController
//public class PostController {
//    private final UserRepository ur;
//    private final PostService ps;

//    @PostMapping("/save")
//    public BaseResponse save(
//            @AuthenticationPrincipal AuthUserDetails user,
//            @ModelAttribute PostDto.ReqPost dto) {
//
//        String email = user.getEmail();
//        User writer = ur.findByEmail(email).orElseThrow(
//                () -> new RuntimeException("사용자를 찾을수 없습니다.")
//        );
//        dto.setUser(user);
//        PostDto.ResPost result =  ps.save(dto);
//
//        return BaseResponse.success(ResponseEntity.ok(result));
//    }
//
//    @GetMapping("/read/{idx}")
//    public BaseResponse read(
//            @AuthenticationPrincipal AuthUserDetails user,
//            @PathVariable("idx") Long post_idx) {
//
//        Long check_user = user.getIdx();
//        ps.read(post_idx, check_user);
//
//        return BaseResponse.success(ResponseEntity.ok("Read 성공"));
//    }
//    @GetMapping("/list")
//    public BaseResponse read(
//            @AuthenticationPrincipal AuthUserDetails user) {
//
//        Long check_user = user.getIdx();
//        List<PostDto.ResList> result = ps.list(check_user);
//
//        return BaseResponse.success(ResponseEntity.ok(result));
//    }
//}
