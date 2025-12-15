package in.vipinshivhare.invoicegeneratorapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME}")
    private String senderName;

    private static final String BREVO_URL =
            "https://api.brevo.com/v3/smtp/email";

    // 🔥 ASYNC + HTTP (Render-safe)
    @Async
    public void sendInvoiceEmailAsync(
            String toEmail,
            byte[] fileBytes,
            String filename
    ) {
        try {
            sendInternal(toEmail, fileBytes, filename);
            log.info("Brevo email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Brevo email failed for {}", toEmail, e);
        }
    }

    private void sendInternal(
            String toEmail,
            byte[] fileBytes,
            String filename
    ) {
        String base64File = Base64.getEncoder().encodeToString(fileBytes);

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "email", senderEmail,
                        "name", senderName   // e.g. "Invoice Generator"
                ),
                "to", List.of(
                        Map.of("email", toEmail)
                ),
                "subject", "Your Invoice is Ready 🧾",
                "htmlContent",
                """
                <div style="font-family: Arial, Helvetica, sans-serif; 
                            max-width: 600px; 
                            margin: 0 auto; 
                            border: 1px solid #e5e7eb; 
                            border-radius: 8px; 
                            overflow: hidden;">
                            
                    <div style="background-color: #111827; 
                                color: #ffffff; 
                                padding: 16px 24px;">
                        <h2 style="margin: 0;">Invoice Generated</h2>
                    </div>
        
                    <div style="padding: 24px; color: #111827;">
                        <p style="font-size: 16px;">
                            Dear Customer,
                        </p>
        
                        <p style="font-size: 14px; line-height: 1.6;">
                            Thank you for your business. Please find your invoice attached to this email.
                            If you have any questions regarding this invoice, feel free to reach out to us.
                        </p>
        
                        <div style="margin: 20px 0; 
                                    padding: 16px; 
                                    background-color: #f9fafb; 
                                    border-left: 4px solid #2563eb;">
                            <p style="margin: 0; font-size: 14px;">
                                📎 <strong>Attachment:</strong> Your invoice PDF
                            </p>
                        </div>
        
                        <p style="font-size: 14px;">
                            We appreciate your trust and look forward to serving you again.
                        </p>
        
                        <p style="margin-top: 32px; font-size: 14px;">
                            Best regards,<br>
                            <strong>%s</strong>
                        </p>
                    </div>
        
                    <div style="background-color: #f3f4f6; 
                                padding: 12px 24px; 
                                font-size: 12px; 
                                color: #6b7280; 
                                text-align: center;">
                        This is an automated email. Please do not reply directly.
                    </div>
                </div>
                """.formatted(senderName),
                "attachment", List.of(
                        Map.of(
                                "content", base64File,
                                "name", filename
                        )
                )
        );


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        BREVO_URL,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "Brevo API error: " + response.getBody()
            );
        }
    }
}
