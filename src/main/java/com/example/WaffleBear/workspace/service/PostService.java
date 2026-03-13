package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.email.EmailVerify;
import com.example.WaffleBear.email.EmailVerifyRepository;
import com.example.WaffleBear.email.EmailVerifyService;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.workspace.model.post.isShare;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
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

    public PostDto.ResPost save(PostDto.ReqPost dto, User user) {

        Post result;

        if(dto.getIdx() != null) {
            // 해당 게시글을 업데이트 할 때
            result = pr.findById(dto.getIdx())
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
            result.update(dto.getTitle(), dto.getContents());
            pr.save(result);
        }else {
            // 처음 게시글 만들 때
            result = new Post();
            result.update(dto.getTitle(), dto.getContents());
            result.setUUID(UUID.randomUUID().toString());

            pr.save(result);
            upr.save(new UserPostDto.ReqUserPost().toEntity(result, user));
        }

        return PostDto.ResPost.from(result);
    }
    public PostDto.ResPost read(Long post_idx, Long check_user) {

        Post result = pr.findById(post_idx).orElseThrow(
                () -> new RuntimeException("파일이 없습니다.")
        );
        System.out.println(result.getTitle());
        System.out.println(result.getContents());
        // UserPost랑 Post랑 관계 맺기
        UserPost userPost = upr.findByUser_IdxAndWorkspace_Idx(
                check_user, post_idx).orElseThrow(null);

        if(userPost != null) {
            return PostDto.ResPost.from(result);
        }else {
            return null;
        }
    }
    public Optional<BaseResponse> delete(Long post_idx, Long check_user) {

        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(check_user, post_idx).orElseThrow(
                () -> new RuntimeException("권한이 없습니다.")
        );
        if(result.getLevel().equals(AccessRole.ADMIN)) {
            pr.deleteById(post_idx);

            return Optional.of(BaseResponse.success(SUCCESS));
        }else {
            return Optional.of(BaseResponse.fail(REQUEST_ERROR));
        }
    }
    public Optional<BaseResponse> invite(String uuid, String email) {

        Post post = pr.findByUUID(uuid).orElseThrow(
                () -> new RuntimeException("파일이 없습니다.")
        );
        User user = ur.findByEmail(email).orElseThrow(
                () -> new RuntimeException("해당하는 유저가 없거나 권한이 없습니다.")
        );

        if(email != null) {
            ur.findByEmail(user.getEmail()).orElseThrow(
                    () -> new RuntimeException("아이디가 없습니다. 회원가입을 하세요.")
            );

            evr.save(new EmailVerify(uuid, email));
            evs.sendVerificationEmail(email, user.getName(), uuid);
            return Optional.of(BaseResponse.success("초대 성공"));
        }

        Optional<UserPost> result = upr.findByUser_IdxAndWorkspace_Idx(user.getIdx(), post.getIdx());

        if (post.getStatus() != isShare.Private && !result.isPresent()) {
            upr.save(UserPost.builder()
                    .user(user)
                    .workspace(post)
                    .Level(AccessRole.READ)
                    .build()
            );
            return Optional.of(BaseResponse.success("초대 성공"));
        }
        return Optional.empty();
    }

    public Optional<BaseResponse> verifyEmail(User user, String uuid, String type) {

        EmailVerify verificationToken = evr.findByToken(uuid).orElseThrow(
                () -> new RuntimeException("유효하지 않은 토큰입니다.")
        );

        if(!verificationToken.getEmail().equals(user.getEmail())) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }else if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            evr.delete(verificationToken);
            throw new RuntimeException("토큰이 만료되었습니다.");
        }
        if(type.equals("reject")) {
            evr.delete(verificationToken);
            throw new RuntimeException("거절하였습니다.");
        }

        // 3. 인증 완료된 토큰 삭제 (재사용 방지)
        evr.delete(verificationToken);
        upr.save(UserPost.builder()
                .user(user)
                .workspace(pr.findByUUID(uuid).orElseThrow())
                .Level(AccessRole.READ)
                .build()
        );

        return Optional.of(BaseResponse.success("초대 성공"));
    }

    public void isShared(Long post_idx, Long check_user, PostDto.ReqType dto) {


        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(check_user, post_idx)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        result.getWorkspace().typeUpdate(dto.getType(), dto.getStatus());

        pr.save(result.getWorkspace());
    }

    public List<UserPostDto.ReqRole> loadRole(Long post_idx, Long user_idx) {

        UserPost result = upr.findByUser_IdxAndWorkspace_Idx(user_idx, post_idx)
                .orElseThrow(() -> new RuntimeException("해당 권한이 없습니다."));

        List<UserPost> load = upr.findAllByWorkspace_idx(post_idx);


        return load.stream().map(UserPostDto.ReqRole::from).toList();
    }

    public List<PostDto.ResList> list(Long user_idx) {

        List<Post> postList = pr.findAllByUserId(user_idx);

        return postList.stream().map(PostDto.ResList::from).toList();
    }
}
