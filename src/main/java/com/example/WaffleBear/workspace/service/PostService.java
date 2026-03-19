package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.email.EmailVerify;
import com.example.WaffleBear.email.EmailVerifyRepository;
import com.example.WaffleBear.email.EmailVerifyService;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.asset.WorkspaceAssetService;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.workspace.model.post.isShare;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.WaffleBear.common.model.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final EmailVerifyRepository evr;
    private final EmailVerifyService evs;
    private final UserRepository ur;
    private final PostRepository pr;
    private final UserPostRepository upr;
    private final NotificationService ns;
    private final WorkspaceAssetService workspaceAssetService;

    // ─────────────────────────────────────────────────────────────────────────
    // 저장 / 수정
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PostDto.ResPost save(PostDto.ReqPost dto, User user) {
        Post result;

        if (dto.idx() != null) {
            result = pr.findById(dto.idx())
                    .orElseThrow(() -> new BaseException(WORKSPACE_NOT_FOUND));
            result.update(dto.title(), dto.contents());
            pr.save(result);
        } else {
            result = new Post();
            result.update(dto.title(), dto.contents());
            result.setUUID(UUID.randomUUID().toString());

            pr.save(result);
            upr.save(new UserPostDto.ReqUserPost(null, null).toEntity(result, user));
        }

        AccessRole accessRole = upr.findByUser_IdxAndWorkspace_Idx(user.getIdx(), result.getIdx())
                .map(UserPost::getLevel)
                .orElse(AccessRole.ADMIN);

        return PostDto.ResPost.from(result, accessRole);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 단건 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PostDto.ResPost read(Long postIdx, Long checkUser) {
        Post result = pr.findById(postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_NOT_FOUND));

        UserPost userPost = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        return PostDto.ResPost.from(result, userPost.getLevel());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UUID로 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PostDto.ResUuidLookup resolveByUuid(Long userIdx, String uuid) {
        Post workspace = pr.findByUUID(uuid)
                .orElseThrow(() -> new BaseException(WORKSPACE_NOT_FOUND));

        Optional<UserPost> existingAccess =
                upr.findByUser_IdxAndWorkspace_Idx(userIdx, workspace.getIdx());

        if (existingAccess.isPresent()) {
            return PostDto.ResUuidLookup.from(workspace, existingAccess.get().getLevel());
        }

        if (workspace.getStatus() == isShare.Public) {
            User user = ur.findById(userIdx)
                    .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

            UserPost relation = upr.save(UserPost.builder()
                    .user(user)
                    .workspace(workspace)
                    .Level(AccessRole.READ)
                    .build());

            return PostDto.ResUuidLookup.from(workspace, relation.getLevel());
        }

        throw new BaseException(WORKSPACE_NOT_ACCESSIBLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 워크스페이스 삭제 (ADMIN 전용)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BaseResponseStatus delete(Long postIdx, Long checkUser) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (result.getLevel().equals(AccessRole.ADMIN)) {
            workspaceAssetService.deleteAllWorkspaceAssets(result.getWorkspace());
            pr.delete(result.getWorkspace());
            return SUCCESS;
        }

        return FAIL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록에서 워크스페이스 제거 (본인 관계만 삭제)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BaseResponseStatus list_delete(Long postIdx, Long checkUser) {
        upr.deleteByUser_IdxAndWorkspace_Idx(checkUser, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        return SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 초대
    // ─────────────────────────────────────────────────────────────────────────

    public BaseResponseStatus invite(String uuid, String email, AuthUserDetails user) {
        Post post = pr.findByUUID(uuid)
                .orElseThrow(() -> new BaseException(WORKSPACE_NOT_FOUND));

        if (!post.getType()) {
            throw new BaseException(WORKSPACE_SHARE_NOT_ALLOWED);
        }

        // 알림 발송 (이메일이 있을 때)
        if (email != null) {
            User invitee = ur.findByEmail(email)
                    .orElseThrow(() -> new BaseException(USER_NOT_FOUND));
            ns.sendWorkspaceInviteNotification(invitee.getIdx(), uuid, post.getTitle());
        }

        // Shared 상태 + 이메일 초대 → 이메일 인증 링크 발송
        if (email != null && post.getStatus() == isShare.Shared) {
            User invitedUser = ur.findByEmail(email)
                    .orElseThrow(() -> new BaseException(USER_NOT_REGISTERED));

            evr.save(new EmailVerify(uuid, email));
            evs.sendVerificationEmail(email, invitedUser.getName(), uuid);
            return SUCCESS;
        }

        // Public 워크스페이스 → 현재 사용자 즉시 참여
        User checkUser = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        Optional<UserPost> relation =
                upr.findByUser_IdxAndWorkspace_Idx(checkUser.getIdx(), post.getIdx());

        if (post.getStatus() == isShare.Public && relation.isEmpty()) {
            upr.save(UserPost.builder()
                    .user(checkUser)
                    .workspace(post)
                    .Level(AccessRole.READ)
                    .build());
            return SUCCESS;
        }

        return FAIL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 초대 수락 / 거절 처리
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BaseResponseStatus verifyEmail(User user, String uuid, String type) {
        EmailVerify verificationToken = evr.findByToken(uuid)
                .orElseThrow(() -> new BaseException(EMAIL_VERIFY_TOKEN_INVALID));

        if (!verificationToken.getEmail().equals(user.getEmail())) {
            throw new BaseException(WORKSPACE_ACCESS_DENIED);
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            evr.delete(verificationToken);
            throw new BaseException(EMAIL_VERIFY_TOKEN_EXPIRED);
        }

        if (type.equals("reject")) {
            evr.delete(verificationToken);
            throw new BaseException(INVITE_REJECTED);
        }

        Post result = pr.findByUUID(uuid)
                .orElseThrow(() -> new BaseException(WORKSPACE_SHARE_ENDED));

        if (upr.findByUser_IdxAndWorkspace_Idx(user.getIdx(), result.getIdx()).isPresent()) {
            evr.delete(verificationToken);
            throw new BaseException(ALREADY_JOINED);
        }

        evr.delete(verificationToken);

        if (result.getStatus() != isShare.Private) {
            upr.save(UserPost.builder()
                    .user(user)
                    .workspace(result)
                    .Level(AccessRole.READ)
                    .build());
            return SUCCESS;
        }

        return FAIL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 공유 상태 변경
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BaseResponseStatus isShared(Long postIdx, Long checkUser, PostDto.ReqType dto) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_NOT_FOUND));

        result.getWorkspace().typeUpdate(dto.type(), dto.status());
        pr.save(result.getWorkspace());
        return SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserPostDto.ResRole> loadRole(Long postIdx, Long userIdx) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(userIdx, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (!result.getLevel().equals(AccessRole.ADMIN)) {
            throw new BaseException(ADMIN_ONLY_ACTION);
        }

        List<UserPost> load = upr.findAllByWorkspace_idx(postIdx);
        return load.stream().map(UserPostDto.ResRole::from).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 권한 저장
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BaseResponseStatus saveRole(Long postIdx, AuthUserDetails admin, Map<Long, AccessRole> role) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(admin.getIdx(), postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (!result.getLevel().equals(AccessRole.ADMIN)) {
            throw new BaseException(ADMIN_ONLY_ACTION);
        }

        List<Long> userList = new ArrayList<>(role.keySet());

        List<UserPost> updateRole = upr.findAllByWorkspaceIdAndUserIdsExceptAdmin(
                userList, postIdx, admin.getIdx());

        updateRole.forEach(userPost -> {
            Long userIdx = userPost.getUser().getIdx();
            AccessRole newRole = role.get(userIdx);
            userPost.updateLevel(newRole);
        });

        return SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PostDto.ResList> list(Long userIdx) {
        List<UserPost> relationList =
                upr.findAllByUser_IdxOrderByWorkspaceUpdatedAtDesc(userIdx);
        return relationList.stream().map(PostDto.ResList::from).toList();
    }
}