package ijiri.ijiriserver.domain.auth.email.client;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

    private static final String SUBJECT = "[이지리] 이메일 인증 코드";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendCode(String to, String code, long validMinutes) {
        send(to, "인증 코드: " + code + "\n" + validMinutes + "분 안에 입력해 주세요.");
    }

    // 이미 가입된 이메일로 인증 요청이 오면 API 응답은 똑같이 두고 메일로만 알려, 가입 여부가 노출되지 않게 한다
    public void sendAlreadyRegistered(String to) {
        send(to, "이미 이 이메일로 가입된 계정이 있습니다.\n본인이 요청하지 않았다면 이 메일은 무시해 주세요.");
    }

    public void sendWithdrawnAccount(String to) {
        send(to, "이 이메일은 탈퇴 처리된 계정입니다.\n탈퇴 후 30일이 지나면 다시 가입할 수 있습니다.");
    }

    private void send(String to, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(text);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Verification mail send failed", e);
            throw new CustomException(AuthStatusCode.EMAIL_SEND_FAILED);
        }
    }
}
