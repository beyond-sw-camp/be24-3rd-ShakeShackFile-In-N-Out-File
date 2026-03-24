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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.example.WaffleBear.common.model.BaseResponseStatus.*;

import java.util.List;
import java.util.Map;

@Tag(name = "워크스페이스 (Workspace)", description = "워크스페이스 게시글 CRUD, 초대, 권한 관리 API")
@RequestMapping("/workspace")
@RequiredArgsConstructor
@RestController
public class PostController {

    private final UserRepository ur;
    private final PostService ps;

    // ─────────────────────────────────────────────────────────────────────────
    // 저장 / 수정
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "워크스페이스 저장/수정", description = "새 워크스페이스를 생성하거나 기존 워크스페이스를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/save")
    public BaseResponse save(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody PostDto.ReqPost dto) {

        User writer = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        PostDto.ResPost result = ps.save(dto, writer);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 단건 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "워크스페이스 단건 조회", description = "워크스페이스 ID로 단건 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping("/read/{idx}")
    public BaseResponse read(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx) {

        PostDto.ResPost result = ps.read(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UUID로 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "UUID로 워크스페이스 조회", description = "UUID를 사용하여 워크스페이스를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    @GetMapping("/by-uuid/{uuid}")
    public BaseResponse readByUuid(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 UUID", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable("uuid") String uuid) {

        PostDto.ResUuidLookup result = ps.resolveByUuid(user.getIdx(), uuid);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 워크스페이스 삭제 (ADMIN 전용)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. OWNER 권한이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    })
    @PostMapping("/delete/{idx}")
    public BaseResponse delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx) {

        BaseResponseStatus result = ps.delete(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록에서 워크스페이스 제거
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "목록에서 워크스페이스 제거", description = "내 워크스페이스 목록에서 해당 워크스페이스를 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 제거 성공")
    })
    @PostMapping("/delete/list/{idx}")
    public BaseResponse listDelete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx) {

        BaseResponseStatus result = ps.list_delete(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 초대
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "워크스페이스 초대", description = "UUID를 사용하여 워크스페이스에 사용자를 초대합니다. 이메일을 지정하면 해당 사용자를 초대하고, 미지정 시 본인이 참여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식")
    })
    @PostMapping("/invite")
    public BaseResponse invite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 UUID", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam("uuid") String uuid,
            @Parameter(description = "초대할 사용자 이메일 (선택)", example = "user@example.com") @RequestParam(value = "email", required = false) String email) {

        if (email != null && email.contains("@kakao.social")) {
            return BaseResponse.fail(INVALID_EMAIL_FORMAT);
        }

        BaseResponseStatus result = ps.invite(uuid, email, user);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 초대 수락 / 거절 처리
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "초대 수락/거절", description = "이메일로 받은 워크스페이스 초대를 수락하거나 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/verify")
    public BaseResponse verifyEmail(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 UUID", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam("uuid") String uuid,
            @Parameter(description = "처리 유형 (accept/reject)", example = "accept") @RequestParam("type") String type) {

        User checkUser = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        BaseResponseStatus result = ps.verifyEmail(checkUser, uuid, type);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 공유 상태 변경
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "공유 상태 변경", description = "워크스페이스의 공유 상태를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/isShared/{idx}")
    public BaseResponse isShared(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx,
            @RequestBody PostDto.ReqType dto) {

        BaseResponseStatus result = ps.isShared(postIdx, user.getIdx(), dto);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 조회 / 권한 변경 및 추방
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "참여자 권한 조회", description = "워크스페이스 참여자 목록과 각 권한을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/loadRole/{idx}")
    public BaseResponse loadRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx) {

        List<UserPostDto.ResRole> result = ps.loadRole(postIdx, user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }


    // ─── 단일 유저 역할 변경 ────────────────────────────────────────────────────
    @Operation(summary = "참여자 역할 변경", description = "워크스페이스 참여자의 역할(OWNER/EDITOR/VIEWER)을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "역할 변경 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/{postIdx}/role/{targetUserIdx}")
    public BaseResponse changeSingleRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long postIdx,
            @Parameter(description = "대상 사용자 IDX", example = "5") @PathVariable Long targetUserIdx,
            @RequestBody Map<String, String> body) {

        String role = body.get("role");
        BaseResponseStatus result = ps.changeSingleRole(postIdx, user, targetUserIdx, role);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─── 유저 추방 ──────────────────────────────────────────────────────────────
    @Operation(summary = "참여자 추방", description = "워크스페이스에서 특정 참여자를 추방합니다. OWNER 권한이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추방 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{postIdx}/member/{targetUserIdx}")
    public BaseResponse kickMember(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable Long postIdx,
            @Parameter(description = "추방할 사용자 IDX", example = "5") @PathVariable Long targetUserIdx) {

        BaseResponseStatus result = ps.kickMember(postIdx, user, targetUserIdx);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 저장
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "권한 일괄 저장", description = "워크스페이스 참여자들의 권한을 일괄 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 저장 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/saveRole/{idx}")
    public BaseResponse saveRole(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user,
            @Parameter(description = "워크스페이스 ID", example = "1") @PathVariable("idx") Long postIdx,
            @RequestBody Map<Long, AccessRole> role) {

        BaseResponseStatus result = ps.saveRole(postIdx, user, role);
        return BaseResponse.success(ResponseEntity.ok(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "워크스페이스 목록 조회", description = "현재 사용자가 참여 중인 워크스페이스 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    })
    @GetMapping("/list")
    public BaseResponse list(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUserDetails user) {

        List<PostDto.ResList> result = ps.list(user.getIdx());
        return BaseResponse.success(ResponseEntity.ok(result));
    }
}