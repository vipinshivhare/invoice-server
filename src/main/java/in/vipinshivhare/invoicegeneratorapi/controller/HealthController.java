package in.vipinshivhare.invoicegeneratorapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "UP");
        payload.put("service", "Invoice Generator API");
        payload.put("timestamp", Instant.now().toString());
        payload.put("version", "1.0.0");
        payload.put("message", "Server is running successfully");
        
        return ResponseEntity.ok(payload);
    }
}

