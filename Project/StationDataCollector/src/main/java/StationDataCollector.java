import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

public class StationDataCollector {
    private static final String RPC_QUEUE_NAME = "toStationDataCollector";

    private static int getStationData(String s) {
        int total = 0;
        String[] parts = s.split(" ");
        String customerId = parts[0];

        for (int i = 1; i < parts.length; i++) {
            //maybe error with the lenght
            String stationId = parts[i];
            String dbUrl = "jdbc:postgresql://localhost:3001" + stationId + "/stationdb";

            String user = "postgres";
            String password = "postgres";

            try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SUM(kwh) AS total_kwh FROM charge WHERE customer_id = " + customerId)) {

                if (rs.next()) {
                    total += rs.getInt("total_kwh");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return total;
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (com.rabbitmq.client.Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);
            channel.basicQos(1);

            System.out.println(" [x] Awaiting RPC requests containing UserId");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                int response = 0;
                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    String id = message;

                    System.out.println(" [.] sentId: (" + id + ")");
                    response = getStationData(id);
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e);
                } finally {
                    String strResponse = Integer.toString(response);
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, strResponse.getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
            }));

            // Use CountDownLatch to keep the main thread alive
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        }
    }
}