//package com.example.WaffleBear.posts;
//
//import com.example.WaffleBear.posts.model.Posts;
//import com.example.WaffleBear.posts.model.PostDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class PostService {
//    private final PostRepository pr;
//
//    public PostDto.ResPost save(PostDto.ReqPost dto) {
//
//        Posts result = dto.toEntity();
//
//        result = pr.save(result);
//
//        return PostDto.ResPost.from(result);
//    }
//    public PostDto.ResPost read(Long post_idx, Long check_user) {
//
//        Optional<Posts> result = Optional.ofNullable(pr.findById(post_idx).orElseThrow(
//                () -> new RuntimeException("파일이 없습니다.")
//        ));
//
//        if(result.isPresent()) {
//            result
//            return
//        }else {
//            return null;
//        }
//
//
//        result = pr.save(result);
//
//        return PostDto.ResPost.from(result);
//    }
//
//    public PostDto.ResPost list(Long user_idx) {
//
//
//
//        return PostDto.ResPost.from(result);
//    }
//}
