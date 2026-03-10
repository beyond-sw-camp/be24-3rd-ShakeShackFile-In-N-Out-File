<<<<<<<< HEAD:src/main/java/com/example/WaffleBear/workspace/controller/PostController.java
package com.example.WaffleBear.workspace.controller;

import com.example.WaffleBear.workspace.service.PostService;
========
package com.example.WaffleBear.workspace;

>>>>>>>> fcdc92f ([Refactor] Posts => workspace로 수정):src/main/java/com/example/WaffleBear/workspace/PostController.java
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/workspace")
@RequiredArgsConstructor
@RestController
public class PostController {
    private final UserRepository ur;
    private final PostService ps;

    @PostMapping("/save")
    public BaseResponse save(
            @AuthenticationPrincipal AuthUserDetails user,
            @ModelAttribute PostDto.ReqPost dto) {

<<<<<<<< HEAD:src/main/java/com/example/WaffleBear/workspace/controller/PostController.java
//        String email = user.getEmail();
//        User writer = ur.findByEmail(email).orElseThrow(
//                () -> new RuntimeException("사용자를 찾을수 없습니다.")
//        );

========
        String email = user.getEmail();
        User writer = ur.findByEmail(email).orElseThrow(
                () -> new RuntimeException("사용자를 찾을수 없습니다.")
        );
        dto.setUser(writer);
>>>>>>>> fcdc92f ([Refactor] Posts => workspace로 수정):src/main/java/com/example/WaffleBear/workspace/PostController.java
        PostDto.ResPost result =  ps.save(dto);

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @GetMapping("/read/{idx}")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long post_idx) {

        Long check_user = user.getIdx();
        ps.read(post_idx, check_user);

        return BaseResponse.success(ResponseEntity.ok("Read 성공"));
    }
    @GetMapping("/list")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user) {

        Long check_user = user.getIdx();
        List<PostDto.ResList> result = ps.list(check_user);

        return BaseResponse.success(ResponseEntity.ok(result));
    }
}
