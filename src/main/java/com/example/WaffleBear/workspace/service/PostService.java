package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.common.model.BaseResponse;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.WaffleBear.common.model.BaseResponseStatus.FAIL;
import static com.example.WaffleBear.common.model.BaseResponseStatus.REQUEST_ERROR;
import static com.example.WaffleBear.common.model.BaseResponseStatus.SUCCESS;

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

    @Transactional
    public PostDto.ResPost save(PostDto.ReqPost dto, User user) {
        Post result;

        if (dto.idx() != null) {
            result = pr.findById(dto.idx())
                    .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스가 존재하지 않습니다."));
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

    @Transactional(readOnly = true)
    public PostDto.ResPost read(Long postIdx, Long checkUser) {
        Post result = pr.findById(postIdx).orElseThrow(
                () -> new RuntimeException("워크스페이스를 찾을 수 없습니다.")
        );
        UserPost userPost = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx).orElseThrow(
                () -> new RuntimeException("워크스페이스 접근 권한이 없습니다.")
        );

        return PostDto.ResPost.from(result, userPost.getLevel());
    }

    @Transactional
    public PostDto.ResUuidLookup resolveByUuid(Long userIdx, String uuid) {
        Post workspace = pr.findByUUID(uuid).orElseThrow(
                () -> new RuntimeException("워크스페이스를 찾을 수 없습니다.")
        );

        Optional<UserPost> existingAccess = upr.findByUser_IdxAndWorkspace_Idx(userIdx, workspace.getIdx());
        if (existingAccess.isPresent()) {
            return PostDto.ResUuidLookup.from(workspace, existingAccess.get().getLevel());
        }

        if (workspace.getStatus() == isShare.Public) {
            User user = ur.findById(userIdx).orElseThrow(
                    () -> new RuntimeException("사용자를 찾을 수 없습니다.")
            );

            UserPost relation = upr.save(UserPost.builder()
                    .user(user)
                    .workspace(workspace)
                    .Level(AccessRole.READ)
                    .build());

            return PostDto.ResUuidLookup.from(workspace, relation.getLevel());
        }

        throw new RuntimeException("접근 가능한 워크스페이스가 아닙니다.");
    }

    @Transactional
    public Optional<BaseResponse> delete(Long postIdx, Long checkUser) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx).orElseThrow(
                () -> new RuntimeException("워크스페이스 접근 권한이 없습니다.")
        );
        if (result.getLevel().equals(AccessRole.ADMIN)) {
            workspaceAssetService.deleteAllWorkspaceAssets(result.getWorkspace());
            pr.delete(result.getWorkspace());

            return Optional.of(BaseResponse.success(SUCCESS));
        }
        return Optional.of(BaseResponse.fail(REQUEST_ERROR));
    }

    @Transactional
    public Optional<BaseResponse> list_delete(Long postIdx, Long checkUser) {
        upr.deleteByUser_IdxAndWorkspace_Idx(checkUser, postIdx).orElseThrow(
                () -> new RuntimeException("워크스페이스 접근 권한이 없습니다."));

        return Optional.of(BaseResponse.success(SUCCESS));
    }

    @Async
    public Optional<BaseResponse> invite(String uuid, String email, AuthUserDetails user) {
        Post post = pr.findByUUID(uuid).orElseThrow(
                () -> new RuntimeException("워크스페이스를 찾을 수 없습니다.")
        );
        if (!post.getType()) {
            throw new RuntimeException("이 워크스페이스는 공유 권한이 없습니다.");
        }
        if (email != null) {
            User invitee = ur.findByEmail(email).orElseThrow(
                    () -> new RuntimeException("해당 사용자를 찾을 수 없습니다.")
            );
            ns.sendWorkspaceInviteNotification(invitee.getIdx(), uuid, post.getTitle());
        }

        if (email != null && post.getStatus() == isShare.Shared) {
            User invitedUser = ur.findByEmail(email).orElseThrow(
                    () -> new RuntimeException("해당 사용자를 찾을 수 없습니다.")
            );
            ur.findByEmail(invitedUser.getEmail()).orElseThrow(
                    () -> new RuntimeException("가입되지 않은 이메일입니다. 회원가입을 해주세요.")
            );

            evr.save(new EmailVerify(uuid, email));
            evs.sendVerificationEmail(email, invitedUser.getName(), uuid);
            return Optional.of(BaseResponse.success("초대 성공"));
        }

        User checkUser = ur.findByEmail(user.getEmail()).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다.")
        );

        Optional<UserPost> result = upr.findByUser_IdxAndWorkspace_Idx(checkUser.getIdx(), post.getIdx());

        if (post.getStatus() == isShare.Public && result.isEmpty()) {
            upr.save(UserPost.builder()
                    .user(checkUser)
                    .workspace(post)
                    .Level(AccessRole.READ)
                    .build()
            );
            return Optional.of(BaseResponse.success("초대 성공"));
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<BaseResponse> verifyEmail(User user, String uuid, String type) {
        EmailVerify verificationToken = evr.findByToken(uuid).orElseThrow(
                () -> new RuntimeException("유효하지 않은 토큰입니다.")
        );

        if (!verificationToken.getEmail().equals(user.getEmail())) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        } else if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            evr.delete(verificationToken);
            throw new RuntimeException("토큰이 만료되었습니다.");
        }
        if (type.equals("reject")) {
            evr.delete(verificationToken);
            throw new RuntimeException("초대를 거절했습니다.");
        }
        Post result = pr.findByUUID(uuid).orElseThrow(
                () -> new RuntimeException("워크스페이스 공유가 종료되었습니다.")
        );
        if (upr.findByUser_IdxAndWorkspace_Idx(user.getIdx(), result.getIdx()).isPresent()) {
            evr.delete(verificationToken);
            throw new RuntimeException("이미 참여 중인 사용자입니다.");
        }

        evr.delete(verificationToken);
        if (result.getStatus() != isShare.Private) {
            upr.save(UserPost.builder()
                    .user(user)
                    .workspace(result)
                    .Level(AccessRole.READ)
                    .build()
            );
            return Optional.of(BaseResponse.success("초대 성공"));
        }

        return Optional.of(BaseResponse.fail(FAIL));
    }

    @Transactional
    public void isShared(Long postIdx, Long checkUser, PostDto.ReqType dto) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(checkUser, postIdx)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스가 존재하지 않습니다."));

        result.getWorkspace().typeUpdate(dto.type(), dto.status());
        pr.save(result.getWorkspace());
    }

    @Transactional(readOnly = true)
    public List<UserPostDto.ResRole> loadRole(Long postIdx, Long userIdx) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(userIdx, postIdx)
                .orElseThrow(() -> new RuntimeException("해당 워크스페이스에 접근할 수 없습니다."));
        if (!result.getLevel().equals(AccessRole.ADMIN)) {
            throw new RuntimeException("관리자만 권한을 변경할 수 있습니다.");
        }

        List<UserPost> load = upr.findAllByWorkspace_idx(postIdx);
        return load.stream().map(UserPostDto.ResRole::from).toList();
    }

    @Transactional
    public BaseResponseStatus saveRole(Long postIdx, AuthUserDetails admin, Map<Long, AccessRole> role) {
        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(admin.getIdx(), postIdx)
                .orElseThrow(() -> new RuntimeException("해당 워크스페이스에 접근할 수 없습니다."));
        if (!result.getLevel().equals(AccessRole.ADMIN)) {
            throw new RuntimeException("관리자만 권한을 변경할 수 있습니다.");
        }
        List<Long> userList = new ArrayList<>(role.keySet());

        List<UserPost> updateRole =
                upr.findAllByWorkspaceIdAndUserIdsExceptAdmin(
                        userList,
                        postIdx,
                        admin.getIdx()
                );

        updateRole.forEach(userPost -> {
            Long userIdx = userPost.getUser().getIdx();
            AccessRole newRole = role.get(userIdx);
            userPost.updateLevel(newRole);
        });

        return SUCCESS;
    }

    @Transactional(readOnly = true)
    public List<PostDto.ResList> list(Long userIdx) {
        List<UserPost> relationList = upr.findAllByUser_IdxOrderByWorkspaceUpdatedAtDesc(userIdx);
        return relationList.stream().map(PostDto.ResList::from).toList();
    }
}
