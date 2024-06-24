package at.ldjr.springbootapp;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectionFactory connectionFactory; // Mock ConnectionFactory for RabbitMQ

    @Test
    public void testStartDataGatheringJob() throws Exception {
        String customerId = "12345";

        MvcResult result = mockMvc.perform(post("/invoices/post/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(content().string("Data gathering job started for customer " + customerId))
                .andReturn();

        // Additional assertions can be made on the result if necessary
    }

    @Test
    public void testGetInvoice() throws Exception {
        String customerId = "12345";
        Path invoicePath = Paths.get("receipts/Invoice_customer-", customerId + ".pdf");

        // Mock the file existence check
        when(Files.exists(invoicePath)).thenReturn(true);

        MvcResult result = mockMvc.perform(get("/invoices/get/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(content().string("Invoice for customer " + customerId))
                .andReturn();

        // Additional assertions can be made on the result if necessary
    }
}