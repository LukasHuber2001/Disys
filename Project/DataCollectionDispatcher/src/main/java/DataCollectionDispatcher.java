import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;


public class DataCollectionDispatcher {

    private static final String RPC_QUEUE_NAME = "toDataCollectionDispatcher";

    private static String getAvailableStations() {
        List<String> stationIds = new ArrayList<>();
        String dbUrl = "jdbc:postgresql://localhost:30002/stationdb"; // Adjust the DB URL if necessary
        String user = "postgres";
        String password = "postgres";


        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM station")) {

            while (rs.next()) {
                stationIds.add(rs.getString("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(String.join(" ", stationIds));
        return String.join(" ", stationIds);
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);
            channel.basicQos(1);

            System.out.println(" [x] Awaiting RPC requests");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                String response = "";
                try {
                    System.out.println(" [.] Started Dispatching Job");
                    response = getAvailableStations();
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e);
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
            }));
        }
    }
}
