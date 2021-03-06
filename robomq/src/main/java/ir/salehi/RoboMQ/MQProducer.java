package ir.salehi.RoboMQ;

import android.os.Handler;
import android.util.Log;

import com.rabbitmq.client.MessageProperties;
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

public class MQProducer extends MQConnector {
    private static final String TAG = "MQProducer";
    private MQCallback mCallback;
    private String mQueueName;
    private String mExchange;
    private String mRoutingKey;
    private Thread publishThread;
    int publishTimeout = 5 * 1000;
    private Handler mCallbackHandler = new Handler();


    public static MQProducer createInstance(MQFactory factory, MQCallback callback) {
        return new MQProducer(factory.getHostName(),
                factory.getPort(),
                factory.getRoutingKey(),
                factory.getExcahnge(),
                callback);
    }

    private MQProducer(String host, int port, String routingKey, String exchange, MQCallback callback) {
        super(host, port);
        this.mCallback = callback;
        this.mRoutingKey = routingKey;
        this.mExchange = exchange;
        this.mQueueName = createDefaultRoutingKey();
    }

    private String createDefaultRoutingKey() {
        return mRoutingKey + "@" + UUID.randomUUID();
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

    public void publish(final String message) {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        initConnection();
                        initchanenel();
                        declareQueue();
                        mChannel.confirmSelect();
                        mChannel.queueBind(mQueueName, mExchange, mRoutingKey);
                        byte[] messageBytes = message.getBytes();
                        mChannel.basicPublish(mExchange, mRoutingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBytes);
                        mChannel.waitForConfirms(publishTimeout);
                        closeMQConnection();
                        running = false;
                    } catch (InterruptedException | IOException | TimeoutException e) {
                        sendBackErrorMessage(e);
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            running = false;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        publishThread.start();
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

    private void declareQueue() throws IOException {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x-expires", 2 * 60 * 60 * 1000);
        mChannel.queueDeclare(mQueueName, true, false, false, params);
        Log.d(TAG, "Queue :" + "queue:name:" + mQueueName + " declared");
    }

    @Override
    protected ShutdownListener createShutDownListener() {
        ShutdownListener listener = new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause) {
                String errorMessage = cause.getMessage() == null ? "produser connection was shutdown" : "consumer " + cause.getMessage();
                mCallback.onMQConnectionClosed(errorMessage);
            }
        };
        return listener;
    }


    public int getPublishTimeout() {
        return publishTimeout;
    }

    public void setPublishTimeout(int publishTimeout) {
        this.publishTimeout = publishTimeout;
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
