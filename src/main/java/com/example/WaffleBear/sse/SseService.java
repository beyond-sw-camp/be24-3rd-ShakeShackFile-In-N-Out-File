//package com.example.WaffleBear.sse;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class SseService {
//
//    // 유저별 SseEmitter 관리 (userId → SseEmitter)
//    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
//
//    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분
//
//    public SseEmitter connect(Long userIdx) {
//        SseEmitter emitter = new SseEmitter(TIMEOUT);
//
//        // 연결 종료/타임아웃 시 확실히 제거
//        emitter.onCompletion(() -> emitters.remove(userIdx));
//        emitter.onTimeout(() -> emitters.remove(userIdx));
//        emitter.onError(e -> emitters.remove(userIdx));
//
//        emitters.put(userIdx, emitter);
//
//        // 연결 즉시 'init' 이벤트를 보내 연결 성공을 알림
//        try {
//            emitter.send(SseEmitter.event()
//                    .name("connect")
//                    .data("connected"));
//        } catch (IOException e) {
//            emitters.remove(userIdx);
//        }
//
//        return emitter;
//    }
//
//    // 특정 유저에게 이벤트 전송
//    public void sendToUser(Long userId, String eventName, Object data) {
//        SseEmitter emitter = emitters.get(userId);
//        if (emitter == null) return;
//
//        try {
//            emitter.send(SseEmitter.event()
//                    .name(eventName)
//                    .data(data));
//        } catch (IOException e) {
//            emitters.remove(userId);
//            emitter.completeWithError(e);
//        }
//    }
//
//    public boolean isConnected(Long userId) {
//        return emitters.containsKey(userId);
//    }
//}