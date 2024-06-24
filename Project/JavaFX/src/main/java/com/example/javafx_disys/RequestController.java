package com.example.javafx_disys;


import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

public class RequestController extends Application {
    @FXML
    private Button gatherDataButton;
    @FXML
    private Button downloadInvoiceButton;
    @FXML
    private TextField customerIdField1;
    @FXML
    private TextField customerIdField2;
    @FXML
    private Label invoiceInfo;
    @FXML
    private TextArea createResponseInvoice;


    @FXML
    public void initialize() {
        gatherDataButton.setOnAction(event -> gatherData(customerIdField1.getText()));
        downloadInvoiceButton.setOnAction(event -> downloadInvoice(customerIdField2.getText()));

    }

    @FXML
    private void gatherData(String customerId) {
        if (!customerId.isEmpty() && Pattern.matches("^[0-9]+$", customerId)) {

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("http://localhost:8080/invoices/post/%s", customerId)))
                    .POST(HttpRequest.BodyPublishers.ofString(customerId))
                    .build();

            try {
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(body -> Platform.runLater(() -> invoiceInfo.setText(body)))
                        .join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (customerId.isEmpty()) {
            invoiceInfo.setText("The field is empty");
        } else {
            invoiceInfo.setText("The field should only contain numbers");
        }
    }

    private void downloadInvoice(String customerId) {
        if (!customerId.isEmpty() && Pattern.matches("^[0-9]+$", customerId)) {

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("http://localhost:8080/invoices/get/%s", customerId)))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = response.body();
                    Platform.runLater(() -> createResponseInvoice.setText(responseBody));
                    createResponseInvoice.setVisible(true);
                    try {
                        File file = new File("../File Storage\", customerId + \".pdf");
                        HostServices hostServices = getHostServices();
                        hostServices.showDocument(file.getAbsolutePath());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.err.println();
                    createResponseInvoice.setText("HTTP Error: " + statusCode);
                    createResponseInvoice.setVisible(true);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (customerId.isEmpty()) {
            createResponseInvoice.setText("The field is empty");
            createResponseInvoice.setVisible(true);

        } else {
            createResponseInvoice.setText("The field should only contain numbers");
            createResponseInvoice.setVisible(true);

        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        // muss wegen Application eingefügt sein
    }
}
