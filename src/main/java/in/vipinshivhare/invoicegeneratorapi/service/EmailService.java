package in.vipinshivhare.invoicegeneratorapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from:}")
    private String fromEmail;

    // Brevo/Sender requires the sender to match the authenticated user
    @Value("${spring.mail.username:}")
    private String smtpUserEmail;

    public void sendInvoiceEmail(String toEmail, MultipartFile file) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // Use SMTP user as the actual sender to satisfy provider/DMARC policies.
        // Expose the desired fromEmail as Reply-To so customers can respond there.
        String sender = resolveSenderEmail();
        log.info("Preparing invoice email | to={} | sender={} | replyTo={}", toEmail, sender,
                hasText(fromEmail) && !fromEmail.equalsIgnoreCase(sender) ? fromEmail : sender);
        helper.setFrom(sender);
        if (hasText(fromEmail) && !fromEmail.equalsIgnoreCase(sender)) {
            helper.setReplyTo(fromEmail);
        }

        helper.setTo(toEmail);
        helper.setSubject("Your Invoice");
        helper.setText("Dear Customer,\n\nPlease find attached your invoice.\n\nThank you!");

        helper.addAttachment(file.getOriginalFilename(), new ByteArrayResource(file.getBytes()));

        try {
            mailSender.send(message);
            log.info("Invoice email sent successfully to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send invoice email | to={} | sender={} | reason={}", toEmail, sender,
                    ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Provider-safe sender resolution. Always fall back to SMTP user.
     */
    private String resolveSenderEmail() {
        if (hasText(smtpUserEmail)) {
            return smtpUserEmail;
        }
        if (hasText(fromEmail)) {
            return fromEmail;
        }
        throw new IllegalStateException("Email sender not configured (MAIL_USERNAME or MAIL_FROM missing)");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
