package at.ldjr.springbootapp;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    public static String startMQ = "startMQ";

    @PostMapping("/post/{customerId}")
    public ResponseEntity<String> startDataGatheringJob(@PathVariable String customerId) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.queueDeclare(startMQ, false, false, false, null);
                String message = customerId;
                channel.basicPublish("", startMQ, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println(" [x] Sent '" + message + "'");
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok("Data gathering job started for customer " + customerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error starting data gathering job: " + e.getMessage());
        }
    }

    @GetMapping("/get/{customerId}")
    public ResponseEntity<?> getInvoice(@PathVariable String customerId) {
        // Define the path to the invoice PDF
        Path invoicePath = Paths.get("receipts/Invoice_customer-" + customerId + ".pdf");

        // Check if the invoice PDF exists
        if (Files.exists(invoicePath)) {
            try {
                // Read the PDF file as a byte array
                byte[] pdfBytes = Files.readAllBytes(invoicePath);

                // Create a ByteArrayResource from the byte array
                ByteArrayResource resource = new ByteArrayResource(pdfBytes);

                // Set the appropriate headers for the response
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Invoice_customer-" + customerId + ".pdf");
                headers.setContentType(MediaType.APPLICATION_PDF);

                // Return the file in the response body
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfBytes.length)
                        .body(resource);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading invoice file: " + e.getMessage());
            }
        } else {
            // If the invoice PDF does not exist, return a 404 Not Found status
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invoice not found for customer " + customerId);
        }
    }
}
