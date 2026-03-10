package com.example.WaffleBear.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerifyService {
    private final JavaMailSender mailSender;

    @Async // 메일 발송은 시간이 걸리므로 비동기 처리를 권장합니다.
    public void sendVerificationEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[WaffleBear] 회원가입 이메일 인증");

            // 실제 서비스 시에는 localhost 대신 실제 도메인을 넣어야 합니다.
            String verificationLink = "http://localhost:8080/user/verify?token=" + token;

            String htmlContent = String.format(
                    "<h1>WaffleBear 회원가입을 축하합니다.</h1>" +
                            "<p>아래 링크를 클릭하여 인증을 완료해 주세요.</p>" +
                            "<a href='%s'>이메일 인증하기</a>",
                    verificationLink
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            // 로깅 후 예외 처리 전략 필요 (냉정하게 분석하자면, 실패 시 재시도 로직이나 DB 마킹이 수반되어야 함)
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}
