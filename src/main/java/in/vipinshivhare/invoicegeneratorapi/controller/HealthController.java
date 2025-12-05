package in.vipinshivhare.invoicegeneratorapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> payload = Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(payload);
    }
}

