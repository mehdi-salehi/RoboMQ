package ir.salehi.RoboMQ;

import android.os.Handler;
import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class MQConsumer extends MQConnector {
    private static final String TAG = "MQConsumer";
    private MQCallback mCallback;
    private Thread subscribeThread;
    private Consumer mQueue;
    private String mQueueName;
    private String mExchange;
    private String mRoutingKey;

    private Handler mCallbackHandler = new Handler();
    private MQConsumerListener mqConsumerListener;

    public interface MQConsumerListener {
        public void onMessageReceived(long delivery);
    }

    public static MQConsumer createInstance(MQFactory factory, MQCallback callback) {
        return new MQConsumer(factory.getHostName(),
                factory.getPort(),
                factory.getRoutingKey(),
                factory.getExcahnge(),
                callback
        );
    }

    private MQConsumer(String host, int port, String routingKey, String excahnge, MQCallback callback) {
        super(host, port);
        this.mRoutingKey = routingKey;
        this.mExchange = excahnge;
        this.mCallback = callback;
        this.mQueueName = createDefaultQueueName();
    }

    public void setMessageListner(MQConsumerListener listner) {
        mqConsumerListener = listner;
    }

    ;

    public String createDefaultQueueName() {
        return mRoutingKey + "@" + UUID.randomUUID();
    }


    @Override
    protected ShutdownListener createShutDownListener() {
        ShutdownListener listener = new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                String errorMessage = cause.getMessage() == null ? "cunsumer connection was shutdown" : "consumer " + cause.getMessage();
                mCallback.onMQConnectionClosed(errorMessage);
            }
        };
        return listener;
    }

    public void stop() {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {

                    try {
                        closeMQConnection();
                    } catch (IOException | TimeoutException e) {
                        sendBackErrorMessage(e);
                        e.printStackTrace();
                    }

                }
            }
        });
        subscribeThread.start();
    }

    public void subscribe() {
        Log.d(TAG, "subscribe");
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        initConnection();
                        initchanenel();
                        declareQueue();
                        mChannel.queueBind(mQueueName, mExchange, mRoutingKey);
                        mQueue = new DefaultConsumer(mChannel) {
                            @Override
                            public void handleDelivery(String consumerTag, final Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                                //super.handleDelivery(consumerTag, envelope, properties, body);
                                String routingKey = envelope.getRoutingKey();
                                String contentType = properties.getContentType();
                                final long deliveryTag = envelope.getDeliveryTag();
                                while (isListening) {
                                    mChannel.basicAck(deliveryTag, false);
                                    mCallbackHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mqConsumerListener.onMessageReceived(deliveryTag);
                                        }
                                    });
                                }
                            }
                        };
                        mChannel.basicConsume(mQueueName, mQueue);

                    } catch (ConsumerCancelledException
                            | ShutdownSignalException | IOException | TimeoutException e) {
                        sendBackErrorMessage(e);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        subscribeThread.start();
    }

    private void sendBackErrorMessage(Exception e) {
        final String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
        mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onMQConnectionFailure(errorMessage);
            }
        });
    }


    private void initConnection() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        if (!isConnected()) {
            createConnection();
        }
    }


    private void initchanenel() {
        if (!isChannelAvailable()) {
            createChannel();
        }
    }


    private void declareQueue() throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x-expires", 2 * 60 * 60 * 1000);
        mChannel.queueDeclare(mQueueName, true, false, false, params);
        Log.d(TAG, "Queue :" + "queue:name:" + mQueueName + " declared");
    }

    public String getExchange() {
        return mExchange;
    }

    public void setExchange(String mExchange) {
        this.mExchange = mExchange;
    }

    public String getRoutingkey() {
        return mRoutingKey;
    }

    public void setRoutingkey(String mRoutingKey) {
        this.mRoutingKey = mRoutingKey;
    }

    public String getQueueName() {
        return mQueueName;
    }

    public void setQueueName(String mQueueName) {
        this.mQueueName = mQueueName;
    }
}
