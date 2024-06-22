import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.List;


public class StationDataCollector {
    private static final String RPC_QUEUE_NAME = "toStationDataCollector";
    private static String getStationData(String s){
        if(s.length()==0){
            return "please enter something for me to search for";
        }else{
            List<String> results;


            /*
            SQL Querry
             results.add(Station);
             */
            return "works";
        }
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

            String response = "";
            try {
                String message = new String(delivery.getBody(), "UTF-8");
                String id = message.toString();

                System.out.println(" [.] sentId: (" + message + ")");
                response += getStationData(id);
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
