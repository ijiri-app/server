package ijiri.ijiriserver.domain.auth.email.client;

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VerificationMailClient {

    private static final String SUBJECT = "[이지리] 이메일 인증 코드";

    private final JavaMailSender mailSender;
    private final String from;

    public VerificationMailClient(JavaMailSender mailSender, @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendCode(String to, String code, long validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText("인증 코드: " + code + "\n" + validMinutes + "분 안에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Verification mail send failed", e);
            throw new CustomException(AuthStatusCode.EMAIL_SEND_FAILED);
        }
    }
}
