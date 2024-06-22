import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
public class RpcClient implements AutoCloseable{

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "toDataCollectionDispatcher";
    private String requestQueueName2 = "toStationDataCollector";

    public RpcClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        String customerId = "";
        String stationData;
        try (RpcClient sendCustomerId = new RpcClient()) {
            //gets uid from springboot
            for (int i = 0; i < 32; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [x] Requesting fib(" + i_str + ")");
                customerId = sendCustomerId.callToStationCollectionDispatcher(i_str);
                System.out.println(" [.] Got '" + customerId + "'");
            }


        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        try(RpcClient getStationData = new RpcClient()){
            System.out.println("[x] Requesting Station data for " + customerId);
            stationData = getStationData.callToStationDataCollector(customerId);
            //testing
            System.out.println("[.] Test if this works:" + stationData);
        }catch (IOException | TimeoutException | InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }
    public String callToStationDataCollector(String message) throws IOException, InterruptedException, ExecutionException{
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        channel.basicPublish("", requestQueueName2, props, message.getBytes("UTF-8"));
        final CompletableFuture<String> response = new CompletableFuture<>();

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.complete(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.get();
        channel.basicCancel(ctag);
        return result;
    }
    public String callToStationCollectionDispatcher(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final CompletableFuture<String> response = new CompletableFuture<>();

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.complete(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.get();
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}
