import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.*;


public class DataCollectionReceiver {
    private static final String RPC_QUEUE_NAME = "toDataCollectionReceiver";
    public static String dataCollected = "";
    private static String gatherData(String message){
        // so far the receiver only gets the customer id and the total
        if (dataCollected.length()==0){
            dataCollected = message;
        }else {
            dataCollected += " " + message;
        }
        return dataCollected;
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
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
                String message = new String(delivery.getBody(), "UTF-8");
                String n = message.toString();

                System.out.println(" [.] data Received: (" + message + ")");
                gatherData(n);
                System.out.println("[x] Data gathered so far (" + dataCollected +")");
            } catch (RuntimeException e) {
                System.out.println(" [.] " + e);
            } finally {
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {}));
    }
}