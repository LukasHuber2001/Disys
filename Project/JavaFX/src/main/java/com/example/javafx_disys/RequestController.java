package com.example.javafx_disys;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
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
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> Platform.runLater(() -> invoiceInfo.setText(body)))
                    .join();
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
                HttpResponse<byte[]> response = client.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    byte[] pdfBytes = response.body();
                    Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Invoice PDF");
                        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                        File file = fileChooser.showSaveDialog(downloadInvoiceButton.getScene().getWindow());
                        if (file != null) {
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                fos.write(pdfBytes);
                                createResponseInvoice.setText("Invoice downloaded successfully to " + file.getAbsolutePath());
                            } catch (IOException e) {
                                createResponseInvoice.setText("Error saving invoice: " + e.getMessage());
                            }
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        createResponseInvoice.setText("HTTP Error: " + statusCode);
                        createResponseInvoice.setVisible(true);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createResponseInvoice.setText("Error: " + e.getMessage());
                    createResponseInvoice.setVisible(true);
                });
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
        // This method must be overridden because it is abstract in the Application class
    }

    private static void printPaths(File file) throws IOException {
        System.out.println("Absolute Path: " + file.getAbsolutePath());
        System.out.println("Canonical Path: " + file.getCanonicalPath());
        System.out.println("Path: " + file.getPath());
    }
}
