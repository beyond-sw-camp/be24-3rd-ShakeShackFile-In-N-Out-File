package com.example.WaffleBear.workspace.controller;

import com.example.WaffleBear.common.exception.BaseException;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static com.example.WaffleBear.common.model.BaseResponseStatus.*;
import com.example.WaffleBear.common.exception.BaseException.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/workspace")
@RequiredArgsConstructor
@RestController
public class PostController {

    private final UserRepository ur;
    private final PostService ps;

    // ─────────────────────────────────────────────────────────────────────────
    // 저장 / 수정
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/save")
    public BaseResponse save(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody PostDto.ReqPost dto) {

        User writer = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        PostDto.ResPost result = ps.save(dto, writer);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 단건 조회
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/read/{idx}")
    public BaseResponse read(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        PostDto.ResPost result = ps.read(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UUID로 조회
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-uuid/{uuid}")
    public BaseResponse readByUuid(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("uuid") String uuid) {

        PostDto.ResUuidLookup result = ps.resolveByUuid(user.getIdx(), uuid);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 워크스페이스 삭제 (ADMIN 전용)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/delete/{idx}")
    public BaseResponse delete(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        BaseResponseStatus result = ps.delete(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록에서 워크스페이스 제거
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/delete/list/{idx}")
    public BaseResponse listDelete(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        BaseResponseStatus result = ps.list_delete(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 초대
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/invite")
    public BaseResponse invite(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("uuid") String uuid,
            @RequestParam(value = "email", required = false) String email) {

        if (email != null && email.contains("@kakao.social")) {
            return BaseResponse.fail(INVALID_EMAIL_FORMAT);
        }

        BaseResponseStatus result = ps.invite(uuid, email, user);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 초대 수락 / 거절 처리
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/verify")
    public BaseResponse verifyEmail(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("uuid") String uuid,
            @RequestParam("type") String type) {

        User checkUser = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        BaseResponseStatus result = ps.verifyEmail(checkUser, uuid, type);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 공유 상태 변경
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/isShared/{idx}")
    public BaseResponse isShared(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx,
            @RequestBody PostDto.ReqType dto) {

        BaseResponseStatus result = ps.isShared(postIdx, user.getIdx(), dto);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 조회
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/loadRole/{idx}")
    public BaseResponse loadRole(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx) {

        List<UserPostDto.ResRole> result = ps.loadRole(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 저장
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/saveRole/{idx}")
    public BaseResponse saveRole(
            @AuthenticationPrincipal AuthUserDetails user,
            @PathVariable("idx") Long postIdx,
            @RequestBody Map<Long, AccessRole> role) {

        BaseResponseStatus result = ps.saveRole(postIdx, user, role);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/list")
    public BaseResponse list(
            @AuthenticationPrincipal AuthUserDetails user) {

        List<PostDto.ResList> result = ps.list(user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }
}