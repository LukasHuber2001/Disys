import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;

import com.rabbitmq.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class receiptCreator {

    private static final String RPC_QUEUE_NAME = "toPdfGenerator";

    public static void main(String[] args) throws Exception {
        String regex = "[,\\.\\s]";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");


        try (com.rabbitmq.client.Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);
            channel.basicQos(1);

            System.out.println(" [x] Awaiting Data for PDF Generator");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                //setze user details als variablen
                String customerId;
                String customerTotal;
                String customerFirstName;
                String customerLastName;


                try {
                    System.out.println(" [.] Started PDF generation");
                    String message = new String(delivery.getBody(), "UTF-8");
                    String[] customerData = message.split(regex);
                    customerId = customerData[0];
                    customerTotal = customerData[1];
                    System.out.println(message);
                    String [] customerName = getCustomerName(customerId).split(regex);
                    customerFirstName = customerName[0];
                    customerLastName = customerName[1];
                    createPDF(customerFirstName, customerLastName, customerTotal);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e);
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
            }));
        }
    }

    private static String getCustomerName(String customerId) {
        List<String> stationIds = new ArrayList<>();
        String dbUrl = "jdbc:postgresql://localhost:30001/stationdb"; // Adjust the DB URL if necessary
        String user = "postgres";
        String password = "postgres";

        try (java.sql.Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT first_name,last_name FROM customer WHERE id=?")) {

            while (rs.next()) {
                stationIds.add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.join(" ", stationIds);
    }

        private static void createPDF(String firstName, String lastName, String total) {
            try {
                //setz den path wo die pdf gespeichert werden soll
                String path = "receipts/receipt.pdf";
                File file = new File(path);

                //funktion nur calln wenns probleme mit dem pfad gibt
                //printPaths(file);

                file.getParentFile().mkdirs();

                // gebrauchte variablen, gerade noch mocking data
                Integer receiptNr = 12345;
                // gibt heutiges datum
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDateTime now = LocalDateTime.now();
                //rest der gebrauchten variablen
                String full_name = firstName + ' ' + lastName;
                String station_number = "Station 1";

                // initialisiere pdfwriter
                PdfWriter writer = new PdfWriter(path);

                // initialisiere pdf dokument
                PdfDocument pdf = new PdfDocument(writer);

                // initialisiere dokument
                Document document = new Document(pdf);

                // titel setzen
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                Paragraph title = new Paragraph("Fuel Station Service")
                        .setFont(font)
                        .setFontSize(18)
                        .setMarginBottom(30);
                document.add(title);

                //rechnungsnummer, datum
                Paragraph receiptDetails = new Paragraph()
                        .add(new Text("Receipt No: " + receiptNr + "\n").setTextAlignment(TextAlignment.RIGHT))
                        .add(new Text("Date: " + dtf.format(now)).setTextAlignment(TextAlignment.RIGHT))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setMarginBottom(20);
                document.add(receiptDetails);

                // rechnungstext
                document.add(new Paragraph("\nYour Receipt").setFont(font).setFontSize(16).setMarginBottom(10));
                document.add(new Paragraph("Dear " + full_name).setMarginBottom(10));
                document.add(new Paragraph("Thank you for choosing our Service, down below You will find details about your transaction").setMarginBottom(20));

                // table für die anzeige von den kWh
                float[] columnWidths = {3, 3};
                Table table = new Table(columnWidths)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginTop(10)
                        .setMarginBottom(20);
                // table überschrift
                table.addHeaderCell(new Paragraph("Station Number").setFont(font));
                table.addHeaderCell(new Paragraph("kWh").setFont(font));
                // table content
                table.addCell(station_number);
                table.addCell(total);

                // table zum document hinzufügen
                document.add(table);

                //rest vom rechnungstext
                document.add(new Paragraph("If You have any concerns please do not hesitate to contact our Support Service").setMarginTop(20));
                document.add(new Paragraph("We are happy to be of Service to You again.").setMarginTop(10));

                // dokument schließen
                document.close();

                //printe success message
                System.out.println("Receipt created successfully.");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    //funktion um den speicherpfad von der datei anzuzeigen
    private static void printPaths(File file) throws IOException {
        System.out.println("Absolute Path: " + file.getAbsolutePath());
        System.out.println("Canonical Path: " + file.getCanonicalPath());
        System.out.println("Path: " + file.getPath());
    }
}
