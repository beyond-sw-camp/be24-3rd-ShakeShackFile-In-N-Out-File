package com.example.WaffleBear.workspace.controller;

import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.service.PostService;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("/workspace")
@RequiredArgsConstructor
@RestController
public class PostController {
    private final UserRepository ur;
    private final PostService ps;

    @PostMapping("/save")
    public BaseResponse save(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody PostDto.ReqPost dto) {

        String email = user.getEmail();
        User writer = ur.findByEmail(email).orElseThrow(
                () -> new RuntimeException("사용자를 찾을수 없습니다.")
        );
        System.out.println(dto.getIdx());
        System.out.println(dto.getTitle());
        System.out.println(dto.getContents());

        PostDto.ResPost result =  ps.save(dto, writer);

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @GetMapping("/read/{idx}")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long post_idx) {

        Long check_user = user.getIdx();
        PostDto.ResPost result = ps.read(post_idx, check_user);
        System.out.println(post_idx);
        System.out.println(result.getTitle());
        System.out.println(result.getContents());

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @PostMapping("/delete/{idx}")
    public BaseResponse delete(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long post_idx) {

        Long check_user = user.getIdx();
        Optional<BaseResponse>result = ps.delete(post_idx, check_user);

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @PostMapping("/invite")
    public BaseResponse invite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("uuid") String uuid,
            @RequestParam("type") String type) {

        Long check_user = user.getIdx();
        Optional<BaseResponse> result = ps.invite(uuid, check_user, type);

        if(result.isPresent()) {
            return BaseResponse.success("초대 성공");
        }else {
            return BaseResponse.fail(BaseResponseStatus.FAIL);
        }
    }
    @GetMapping("/verify")
    public BaseResponse verifyEmail(
            @RequestParam("uuid") String uuid,
            @RequestParam("type") String type) {

        if(type != "email") {

            Optional<BaseResponse> result = ps.verifyEmail(uuid);

            if (result.isPresent()) {
                return BaseResponse.success("성공");
            } else {
                return BaseResponse.fail(BaseResponseStatus.FAIL);
            }
        }
        return null;
    }

    @PostMapping("/isShared/{idx}")
    public void isShared(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long post_idx,
            @RequestBody PostDto.ReqType dto) {

        Long check_user = user.getIdx();

        ps.isShared(post_idx, check_user, dto);
    }
    @GetMapping("/loadRole/{idx}")
    public List<UserPostDto.ReqRole> loadRole(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long post_idx) {

        Long check_user = user.getIdx();

        return ps.loadRole(post_idx, check_user);
    }

    @GetMapping("/list")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user) {

        Long check_user = user.getIdx();
        List<PostDto.ResList> result = ps.list(check_user);

        return BaseResponse.success(ResponseEntity.ok(result));
    }
}
