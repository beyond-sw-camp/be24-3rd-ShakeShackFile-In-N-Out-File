//package com.example.WaffleBear.sse;
//
//import com.example.WaffleBear.user.model.AuthUserDetails;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//@RestController
//@RequestMapping("/sse")
//@RequiredArgsConstructor
//public class SseController {
//
//    private final SseService sseService;
//
//    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter connect(@AuthenticationPrincipal AuthUserDetails userDetails) {
//        Long userId = userDetails.getIdx();
//        return sseService.connect(userId);
//    }
//
//
//}