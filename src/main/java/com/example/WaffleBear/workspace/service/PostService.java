package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.common.model.BaseResponse;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.example.WaffleBear.common.model.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository pr;
    private final UserPostRepository upr;

    public PostDto.ResPost save(PostDto.ReqPost dto, User user) {

        Post result;


        if(dto.getIdx() != null) {
            result = pr.findById(dto.getIdx())
                    .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
            result.update(dto.getTitle(), dto.getContents());
            pr.save(result);
        }else {
            result = new Post();
            result.update(dto.getTitle(), dto.getContents());

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

    public List<PostDto.ResList> list(Long user_idx) {

        List<Post> postList = pr.findAllByUserId(user_idx);

        return postList.stream().map(PostDto.ResList::from).toList();
    }
}
