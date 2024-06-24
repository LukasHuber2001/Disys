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
    protected static String dbUrl = "jdbc:postgresql://localhost:30002/postgres";


    private static String getAvailableStations() {
        List<String> stationIds = new ArrayList<>();
<<<<<<< HEAD
=======
        String dbUrl = "jdbc:postgresql://localhost:30002/stationdb"; // Adjust the DB URL if necessary
>>>>>>> ad48d5b9b4eea969493fef73c48c0436c50b212b
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

<<<<<<< HEAD
            com.rabbitmq.client.Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();{
=======
        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        {
>>>>>>> ad48d5b9b4eea969493fef73c48c0436c50b212b
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
<<<<<<< HEAD


=======
>>>>>>> ad48d5b9b4eea969493fef73c48c0436c50b212b
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
