package at.ldjr.springbootapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public InvoiceController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{customerId}")
    public ResponseEntity<String> startDataGatheringJob(@PathVariable String customerId) {
        // Send a message to the RabbitMQ queue
        rabbitTemplate.convertAndSend("yourQueueName", customerId);

        return ResponseEntity.ok("Data gathering job started for customer " + customerId);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<?> getInvoice(@PathVariable String customerId) {
        // Define the path to the invoice PDF
        Path invoicePath = Paths.get("../File Storage", customerId + ".pdf");

        // Check if the invoice PDF exists
        if (Files.exists(invoicePath)) {
            // If it exists, return it along with the download link and creation time
            return ResponseEntity.ok("Invoice for customer " + customerId);
        } else {
            // If it's not, return a 404 Not Found status
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invoice not found for customer " + customerId);
        }
    }
}
