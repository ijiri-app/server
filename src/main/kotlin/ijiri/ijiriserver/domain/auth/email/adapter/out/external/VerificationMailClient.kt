package ijiri.ijiriserver.domain.auth.email.adapter.out.external

import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.domain.auth.email.application.port.out.VerificationMailPort
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class VerificationMailClient(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val from: String,
) : VerificationMailPort {

    override fun sendCode(to: String, code: String, validMinutes: Long) {
        send(to, "인증 코드: $code\n${validMinutes}분 안에 입력해 주세요.")
    }

    // 이미 가입된 이메일로 인증 요청이 오면 API 응답은 똑같이 두고 메일로만 알려, 가입 여부가 노출되지 않게 한다
    override fun sendAlreadyRegistered(to: String) {
        send(to, "이미 이 이메일로 가입된 계정이 있습니다.\n본인이 요청하지 않았다면 이 메일은 무시해 주세요.")
    }

    override fun sendWithdrawnAccount(to: String) {
        send(to, "이 이메일은 탈퇴 처리된 계정입니다.\n탈퇴 후 30일이 지나면 다시 가입할 수 있습니다.")
    }

    private fun send(to: String, text: String) {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(to)
        message.subject = SUBJECT
        message.text = text
        try {
            mailSender.send(message)
        } catch (e: MailException) {
            log.error("Verification mail send failed", e)
            throw CustomException(AuthStatusCode.EMAIL_SEND_FAILED)
        }
    }

    companion object {
        private const val SUBJECT = "[이지리] 이메일 인증 코드"

        private val log = LoggerFactory.getLogger(VerificationMailClient::class.java)
    }
}
