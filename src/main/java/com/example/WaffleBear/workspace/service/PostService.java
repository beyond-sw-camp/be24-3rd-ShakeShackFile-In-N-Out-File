package com.example.WaffleBear.workspace.service;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Workspace;
import com.example.WaffleBear.workspace.model.post.WorkspaceDto;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import com.example.WaffleBear.workspace.model.relation.UserPostDto;
import com.example.WaffleBear.workspace.repository.PostRepository;
import com.example.WaffleBear.workspace.repository.UserPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository pr;
    private final UserPostRepository upr;

    public WorkspaceDto.ResPost save(WorkspaceDto.ReqPost dto, User user) {

        Workspace result = dto.toEntity();
        result = pr.save(result);

        upr.save(new UserPostDto.ReqUserPost().toEntity(result, user));

        return WorkspaceDto.ResPost.from(result);
    }
    public WorkspaceDto.ResPost read(Long post_idx, Long check_user) {

        Workspace result = pr.findById(post_idx).orElseThrow(
                () -> new RuntimeException("파일이 없습니다.")
        );

        if(result.getUserPosts().contains(check_user)) {
            return WorkspaceDto.ResPost.from(result);
        }else {
            return null;
        }
    }

    public List<WorkspaceDto.ResList> list(Long user_idx) {

        List<Workspace> postList = pr.findAllByUserId(user_idx);

        return postList.stream().map(WorkspaceDto.ResList::from).toList();
    }
}
