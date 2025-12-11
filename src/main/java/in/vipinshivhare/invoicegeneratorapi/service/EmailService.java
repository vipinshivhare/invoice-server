package in.vipinshivhare.invoicegeneratorapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from:}")
    private String fromEmail;

    // Fallback when MAIL_FROM is not provided in the environment
    @Value("${spring.mail.username:}")
    private String fallbackFromEmail;

    public void sendInvoiceEmail(String toEmail, MultipartFile file) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(resolveFromEmail());
        helper.setTo(toEmail);
        helper.setSubject("Your Invoice");
        helper.setText("Dear Customer,\n\nPlease find attached your invoice.\n\nThank you!");

        helper.addAttachment(file.getOriginalFilename(), new ByteArrayResource(file.getBytes()));

        mailSender.send(message);
    }

    /**
     * Render deployment sometimes misses MAIL_FROM; fall back to username.
     */
    private String resolveFromEmail() {
        if (hasText(fromEmail)) {
            return fromEmail;
        }
        if (hasText(fallbackFromEmail)) {
            return fallbackFromEmail;
        }
        throw new IllegalStateException("Email sender not configured (MAIL_FROM or MAIL_USERNAME missing)");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
