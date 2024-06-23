import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.List;


public class StationDataCollector {
    private static final String RPC_QUEUE_NAME = "toStationDataCollector";

    private static int getStationData(String s) {
        //test total ammount
        int total = 5;

        // TODO querry db table station and set total to the total ammount of kwh

        return total;
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
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
                String message = new String(delivery.getBody(), "UTF-8");
                String id = message.toString();

                System.out.println(" [.] sentId: (" + id + ")");
                response = getStationData(id);
            } catch (RuntimeException e) {
                System.out.println(" [.] " + e);
            } finally {
                String srtResponse = "" +response;
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, srtResponse.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
        }));
    }
}
