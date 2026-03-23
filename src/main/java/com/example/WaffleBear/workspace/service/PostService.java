package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.sse.SseService;
import com.example.WaffleBear.email.EmailVerify;
import com.example.WaffleBear.email.EmailVerifyRepository;
import com.example.WaffleBear.email.EmailVerifyService;
import com.example.WaffleBear.notification.NotificationService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
//import com.example.WaffleBear.workspace.asset.WorkspaceAssetService;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.example.WaffleBear.common.model.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final SseService sseService;
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
            List<Long> user_list = upr.findUserIdsByPostIdx(result.getIdx());

            // 3. SSE를 통해 참여자들에게 실시간 알림 전송
            sseService.sendTitleUpdate(result.getIdx(), result.getTitle(), user_list);
        } else {
            result = new Post();
            result.update(dto.title(), dto.contents());
            result.setUUID(UUID.randomUUID().toString());

            pr.save(result);
            upr.save(new UserPostDto.ReqUserPost(null, null).toEntity(result, user));

            // 생성 직후에도 동일한 방식으로 SSE 타이틀 전파
            List<Long> user_list = upr.findUserIdsByPostIdx(result.getIdx());
            sseService.sendTitleUpdate(result.getIdx(), result.getTitle(), user_list);
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
                    .Level(AccessRole.WRITE)
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
            Post workspace = result.getWorkspace();
            workspaceAssetService.deleteAllWorkspaceAssets(workspace);
            pr.delete(workspace);
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

            if(!evr.findByToken(uuid).isPresent()) {
                evr.save(new EmailVerify(uuid, email));
            }
            evs.sendVerificationEmail(email, invitedUser.getName(), uuid);
            return SUCCESS;
        }

        // Public 워크스페이스 → 현재 사용자 즉시 참여
        User checkUser = ur.findByEmail(user.getEmail())
                .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

        Optional<UserPost> relation =
                upr.findByUser_IdxAndWorkspace_Idx(checkUser.getIdx(), post.getIdx());

        if(email != null) {
            if(!evr.findByToken(uuid).isPresent()) {
                evr.save(new EmailVerify(uuid, email));
            }
            evs.sendVerificationEmail(email, checkUser.getName(), uuid);
        }

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
            return WORKSPACE_ACCESS_DENIED;
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            evr.delete(verificationToken);
            return EMAIL_VERIFY_TOKEN_EXPIRED;
        }

        if (type.equals("reject")) {
            evr.delete(verificationToken);
            return INVITE_REJECTED; // 또는 기존 정의된 FAIL 등
        }

        Post result = pr.findByUUID(uuid)
                .orElseThrow(() -> new BaseException(WORKSPACE_SHARE_ENDED));

        if (upr.findByUser_IdxAndWorkspace_Idx(user.getIdx(), result.getIdx()).isPresent()) {
            evr.delete(verificationToken);
            return ALREADY_JOINED;
        }

        evr.delete(verificationToken);

        if (result.getStatus() != isShare.Private) {
            upr.save(UserPost.builder()
                    .user(user)
                    .workspace(result)
                    .Level(AccessRole.WRITE)
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
    // 권한 조회 및 권한 변경, 유저 추방
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

    @Transactional
    public BaseResponseStatus changeSingleRole(Long postIdx, AuthUserDetails admin, Long targetUserIdx, String newRole) {
        // 1. 어드민 권한 확인
        UserPost adminPost = upr.findByUser_IdxAndWorkspace_Idx(admin.getIdx(), postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (!adminPost.getLevel().equals(AccessRole.ADMIN)) {
            throw new BaseException(ADMIN_ONLY_ACTION);
        }

        // 2. 대상 유저 역할 변경
        UserPost targetPost = upr.findByUser_IdxAndWorkspace_Idx(targetUserIdx, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        targetPost.updateLevel(AccessRole.valueOf(newRole));

        // 3. SSE로 해당 유저에게 알림 (본인이 같은 페이지에 있으면 새로고침)
        sseService.sendRoleChanged(targetUserIdx, postIdx, newRole);

        return SUCCESS;
    }

    @Transactional
    public BaseResponseStatus kickMember(Long postIdx, AuthUserDetails admin, Long targetUserIdx) {
        // 1. 어드민 권한 확인
        UserPost adminPost = upr.findByUser_IdxAndWorkspace_Idx(admin.getIdx(), postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (!adminPost.getLevel().equals(AccessRole.ADMIN)) {
            throw new BaseException(ADMIN_ONLY_ACTION);
        }

        // 2. 관계 삭제
        upr.deleteByUser_IdxAndWorkspace_Idx(targetUserIdx, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        // 3. SSE로 해당 유저에게 추방 알림
        sseService.sendRoleChanged(targetUserIdx, postIdx, "KICKED");

        return SUCCESS;
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

    @Transactional
    public int shareWithUsers(Long postIdx, Long actorUserIdx, Collection<Long> targetUserIds) {
        UserPost ownerRelation = upr.findByUser_IdxAndWorkspace_Idx(actorUserIdx, postIdx)
                .orElseThrow(() -> new BaseException(WORKSPACE_ACCESS_DENIED));

        if (!ownerRelation.getLevel().equals(AccessRole.ADMIN)) {
            throw new BaseException(ADMIN_ONLY_ACTION);
        }

        Post workspace = ownerRelation.getWorkspace();
        if (!Boolean.TRUE.equals(workspace.getType())) {
            throw new BaseException(WORKSPACE_SHARE_NOT_ALLOWED);
        }

        int affectedCount = 0;
        for (Long targetUserId : targetUserIds == null ? List.<Long>of() : targetUserIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (Objects.equals(actorUserIdx, targetUserId)) {
                continue;
            }

            if (upr.findByUser_IdxAndWorkspace_Idx(targetUserId, postIdx).isPresent()) {
                continue;
            }

            User targetUser = ur.findById(targetUserId)
                    .orElseThrow(() -> new BaseException(USER_NOT_FOUND));

            if(!evr.findByToken(workspace.getUUID()).isPresent()) {
                evr.save(new EmailVerify(workspace.getUUID(), targetUser.getEmail()));
            }
            ns.sendWorkspaceInviteNotification(targetUser.getIdx(), workspace.getUUID(), workspace.getTitle());
            affectedCount += 1;
        }

        return affectedCount;
    }

    @Transactional(readOnly = true)
    public List<PostDto.ResList> list(Long userIdx) {
        List<UserPost> relationList =
                upr.findAllByUser_IdxOrderByWorkspaceUpdatedAtDesc(userIdx);
        return relationList.stream().map(PostDto.ResList::from).toList();
    }
}
