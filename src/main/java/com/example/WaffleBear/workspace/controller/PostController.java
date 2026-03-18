package com.example.WaffleBear.workspace.controller;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;
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
                () -> new RuntimeException("사용자를 찾을 수 없습니다.")
        );

        PostDto.ResPost result = ps.save(dto, writer);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @GetMapping("/read/{idx}")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        Long checkUser = user.getIdx();
        PostDto.ResPost result = ps.read(postIdx, checkUser);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @GetMapping("/by-uuid/{uuid}")
    public BaseResponse readByUuid(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("uuid") String uuid) {

        PostDto.ResUuidLookup result = ps.resolveByUuid(user.getIdx(), uuid);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @PostMapping("/delete/{idx}")
    public BaseResponse delete(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        Long checkUser = user.getIdx();
        Optional<BaseResponse> result = ps.delete(postIdx, checkUser);

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @PostMapping("/delete/list/{idx}")
    public BaseResponse listDelete(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        Long checkUser = user.getIdx();
        Optional<BaseResponse> result = ps.list_delete(postIdx, checkUser);

        return BaseResponse.success(ResponseEntity.ok(result));
    }

    @PostMapping("/invite")
    public BaseResponse invite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("uuid") String uuid,
            @RequestParam(value = "email", required = false) String email) {

        if (email != null && email.contains("@kakao.social")) {
            return BaseResponse.fail(BaseResponseStatus.INVALID_EMAIL_FORMAT);
        }

        Optional<BaseResponse> result = ps.invite(uuid, email, user);

        if (result.isPresent()) {
            return BaseResponse.success("성공");
        }
        return BaseResponse.fail(BaseResponseStatus.FAIL);
    }

    @GetMapping("/verify")
    public BaseResponse verifyEmail(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("uuid") String uuid,
            @RequestParam("type") String type) {

        User checkUser = ur.findByEmail(user.getEmail()).orElseThrow(
                () -> new RuntimeException("해당 사용자가 존재하지 않습니다.")
        );
        Optional<BaseResponse> result = ps.verifyEmail(checkUser, uuid, type);

        if (result.isPresent()) {
            return BaseResponse.success("성공");
        }
        return BaseResponse.fail(BaseResponseStatus.FAIL);
    }

    @PostMapping("/isShared/{idx}")
    public void isShared(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx,
            @RequestBody PostDto.ReqType dto) {

        Long checkUser = user.getIdx();
        ps.isShared(postIdx, checkUser, dto);
    }

    @GetMapping("/loadRole/{idx}")
    public List<UserPostDto.ResRole> loadRole(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        Long checkUser = user.getIdx();
        return ps.loadRole(postIdx, checkUser);
    }

    @PostMapping("/saveRole/{idx}")
    public BaseResponseStatus saveRole(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx,
            @RequestBody Map<Long, AccessRole> role) {

        if (ps.saveRole(postIdx, user, role) != BaseResponseStatus.SUCCESS) {
            return BaseResponseStatus.FAIL;
        }

        return BaseResponseStatus.SUCCESS;
    }

    @GetMapping("/list")
    public BaseResponse list(
            @AuthenticationPrincipal AuthUserDetails user) {

        Long checkUser = user.getIdx();
        List<PostDto.ResList> result = ps.list(checkUser);

        return BaseResponse.success(ResponseEntity.ok(result));
    }
}
