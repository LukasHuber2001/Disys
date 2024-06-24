package at.ldjr.springbootapp;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;


public class RpcClient implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "toDataCollectionDispatcher";
    private String requestQueueName2 = "toStationDataCollector";
    private String requestQueueName3 = "toDataCollectionReceiver";
    private String requestQueueName4 = "toPdfGenerator";
    public static String startMQ = "startMQ";

    public RpcClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {

        //
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(startMQ, false, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String customerId = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received CustomerId: '" + customerId + "'");
                startProcess(customerId);
                System.out.println(" [x] Started MessageQueue");
            };
            channel.basicConsume(startMQ, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
    public static void startProcess(String id){
        String regex = "[,\\.\\s]";
        String availableStations = "";
        String messageReceiverToPdf = "";
        int total = 0;
        try (RpcClient sendDataToReceiver = new RpcClient()) {

            //TODO wenn die query für alle customer daten geht wird hier nicht nur die id sondern alles vom customer übergeben
            sendDataToReceiver.callToDataCollectionReceiver(id);
            System.out.println("Sent Customer Id: "+ id +" to Data Collection Receiver");

            try (RpcClient sendCustomerId = new RpcClient()) {
                System.out.println(" [x] Requesting information on available Stations");
                availableStations = sendCustomerId.callToStationCollectionDispatcher();
                System.out.println(" [.] Available Stations: '" + availableStations + "'");

                String[] myArray = availableStations.split(regex);
                for (String s : myArray) {
                    String idAndStation = id + " " + s;
                    try (RpcClient getStationData = new RpcClient()) {

                        System.out.println("[x] Requesting Station " + s + " data for customer " + id);
                        //für jede station

                        total += Integer.parseInt(getStationData.callToStationDataCollector(idAndStation));
                        System.out.println("[.] total so far :" + total);

                    } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                messageReceiverToPdf = sendDataToReceiver.callToDataCollectionReceiver(Integer.toString(total));
                System.out.println(messageReceiverToPdf);
                System.out.println("your total amounts to " + total + "€");
            } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            try (RpcClient sendDataToPdf = new RpcClient()) {
                sendDataToPdf.callToPdfGenerator(messageReceiverToPdf);
            }
            catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }


    public String callToPdfGenerator(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        channel.basicPublish("", requestQueueName4, props, message.getBytes("UTF-8"));
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
