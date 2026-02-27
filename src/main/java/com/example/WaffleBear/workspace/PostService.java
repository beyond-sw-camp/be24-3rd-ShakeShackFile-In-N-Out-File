package com.example.WaffleBear.workspace;

import com.example.WaffleBear.workspace.model.post.Posts;
import com.example.WaffleBear.workspace.model.post.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository pr;

    public PostDto.ResPost save(PostDto.ReqPost dto) {

        Posts result = dto.toEntity();

        result = pr.save(result);

        return PostDto.ResPost.from(result);
    }
    public PostDto.ResPost read(Long post_idx, Long check_user) {

        Posts result = pr.findById(post_idx).orElseThrow(
                () -> new RuntimeException("파일이 없습니다.")
        );

        if(result.getUser().getIdx() == check_user) {
            return PostDto.ResPost.from(result);
        }else {
            return null;
        }
    }

    public List<PostDto.ResList> list(Long user_idx) {

        List<Posts> postList = pr.findAllByUser_idx(user_idx);

        return postList.stream().map(PostDto.ResList::from).toList();
    }
}
