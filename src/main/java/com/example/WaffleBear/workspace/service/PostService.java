package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import com.example.WaffleBear.workspace.model.post.PostDto;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository pr;
    private final UserPostRepository upr;

    public PostDto.ResPost save(PostDto.ReqPost dto, User user) {

        Post result = pr.findById(dto.getIdx()).orElse(null);


        if(result != null) {
            result.update(dto.getTitle(), dto.getContents());
            pr.save(result);
        }else {
            result = dto.toEntity();
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

    public List<PostDto.ResList> list(Long user_idx) {

        List<Post> postList = pr.findAllByUserId(user_idx);

        return postList.stream().map(PostDto.ResList::from).toList();
    }
}
