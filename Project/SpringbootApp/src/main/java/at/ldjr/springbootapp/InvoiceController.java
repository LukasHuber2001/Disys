package at.ldjr.springbootapp;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

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
            }catch (IOException | TimeoutException e) {
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

