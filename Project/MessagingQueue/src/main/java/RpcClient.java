import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class RpcClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "toDataCollectionDispatcher";
    private String requestQueueName2 = "toStationDataCollector";
    private String requestQueueName3 = "toDataCollectionReceiver";
    private String requestQueueName4 = "toPdfGenerator";

    public RpcClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        //TODO gets customerId from springboot
        //just test data rn
        String regex = "[,\\.\\s]";
        String customerId = "1";
        String availableStations = "";
        String message;
        int total = 0;

        try (RpcClient sendCustomerId = new RpcClient()) {
            System.out.println(" [x] Requesting information on available Stations");
            availableStations = sendCustomerId.callToStationCollectionDispatcher();
            System.out.println(" [.] Available Stations: '" + availableStations + "'");

            String[] myArray = availableStations.split(regex);
            for (String s : myArray) {
                System.out.println(s);
                String idAndStation = customerId + " " + s;
                try (RpcClient getStationData = new RpcClient()) {

                    System.out.println("[x] Requesting Station "+ s +" data for customer " + customerId);
                    total += Integer.parseInt(getStationData.callToStationDataCollector(idAndStation));
                    //test
                    System.out.println("[.] total so far :" + total);


                } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("your total amounts to "+total+"€");
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }/*
        try (RpcClient sendInfoToReceiver = new RpcClient()) {
            System.out.println("[x] Requesting searching data for " + availableStations);
            stationData = sendInfoToReceiver.callToDataCollectionReceiver(customerId);
            //testing
            System.out.println("[.] Test if this works:" + stationData);
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        */
    }


    public String callToDataCollectionReceiver(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        channel.basicPublish("", requestQueueName3, props, message.getBytes("UTF-8"));
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

    public String callToStationDataCollector(String message) throws IOException, InterruptedException, ExecutionException {
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

    public String callToStationCollectionDispatcher() throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, null);

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
