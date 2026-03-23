package com.example.WaffleBear.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerifyService {
    private final JavaMailSender mailSender;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[WaffleBear] 회원가입 이메일 인증");

            String verificationLink = backendUrl + "/user/verify?token=" + token;

            String htmlContent = String.format(
                    "<h1>WaffleBear 회원가입을 축하합니다.</h1>" +
                            "<p>아래 링크를 클릭하여 인증을 완료해 주세요.</p>" +
                            "<a href='%s'>이메일 인증하기</a>",
                    verificationLink
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Async
    public void sendVerificationEmail(String email, String username, String uuid) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[WaffleBear] " + username + "님이 워크스페이스로 초대했습니다.");

            String baseUrl = frontendUrl + "/workspace/verify?uuid=" + uuid;
            String acceptLink = baseUrl + "&type=accept";
            String rejectLink = baseUrl + "&type=reject";

            String htmlContent = String.format(
                    "<div style='font-family: \"Apple SD Gothic Neo\", sans-serif; max-width: 500px; margin: 0 auto; padding: 40px; border: 1px solid #f0f0f0; border-radius: 16px; text-align: center; color: #333;'>" +
                            "  <div style='font-size: 40px; margin-bottom: 20px;'>📩</div>" +
                            "  <h1 style='font-size: 24px; font-weight: 700; margin-bottom: 10px;'>워크스페이스 초대</h1>" +
                            "  <p style='font-size: 16px; line-height: 1.6; color: #666; margin-bottom: 30px;'>" +
                            "    <strong>%s</strong>님이 당신을<br>WaffleBear 워크스페이스로 초대했습니다." +
                            "  </p>" +
                            "  <div style='display: flex; justify-content: center; gap: 10px;'>" +
                            "    <a href='%s' style='display: inline-block; background-color: #3B82F6; color: white; padding: 14px 28px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 15px; box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.5);'>초대 수락하기</a>" +
                            "    <a href='%s' style='display: inline-block; background-color: #F3F4F6; color: #4B5563; padding: 14px 28px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 15px; margin-left: 10px;'>거절</a>" +
                            "  </div>" +
                            "  <p style='margin-top: 30px; font-size: 13px; color: #999;'>이 초대는 보안을 위해 일정 시간이 지나면 만료될 수 있습니다.</p>" +
                            "</div>",
                    username, acceptLink, rejectLink
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}
