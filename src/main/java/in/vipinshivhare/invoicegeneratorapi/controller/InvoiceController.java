package in.vipinshivhare.invoicegeneratorapi.controller;

import in.vipinshivhare.invoicegeneratorapi.entity.Invoice;
import in.vipinshivhare.invoicegeneratorapi.service.EmailService;
import in.vipinshivhare.invoicegeneratorapi.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "https://invoicee-generator.netlify.app, http://localhost:5173") // for frontend access
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService service;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Invoice> saveInvoice(@RequestBody Invoice invoice) {
        return ResponseEntity.ok(service.saveInvoice(invoice));
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> fetchInvoices(Authentication authentication) {
        System.out.println(authentication.getName());
        return ResponseEntity.ok(service.fetchInvoices(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeInvoice(@PathVariable String id, Authentication authentication) {
        if (authentication.getName() != null) {
            service.removeInvoice(authentication.getName(), id);
            return ResponseEntity.noContent().build();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User does not have permission to access this resource");
    }

    @PostMapping(
            value = "/sendinvoice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> sendInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String customerEmail
    ) throws IOException {

        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename();

        emailService.sendInvoiceEmailAsync(
                customerEmail,
                fileBytes,
                filename
        );

        return ResponseEntity.ok("Invoice email successfully!");
    }

}
