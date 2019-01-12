package ir.salehi.RoboMQ;

import android.annotation.SuppressLint;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public abstract class MQConnector {
    protected Channel mChannel = null;
    protected Connection mConnection;

    protected boolean running = true;
    protected boolean isListening = true;

    protected String host;
    protected int port;
    private int requestTimeOut = 30 * 1000;
    private int requestHeartBeat = 30;

    public MQConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public void closeMQConnection() throws IOException, TimeoutException {
        running = false;
        isListening = false;
        if (isConnected()) {
            mConnection.close();
        }
        if (isChannelAvailable()) {
            mChannel.close();
            mChannel.abort();
        }
    }

    @SuppressLint("AuthLeak")
    protected void createConnection() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUri(host);
        connectionFactory.setConnectionTimeout(this.requestTimeOut);
        connectionFactory.setRequestedHeartbeat(this.requestHeartBeat);
        mConnection = connectionFactory.newConnection();
        mConnection.addShutdownListener(createShutDownListener());
    }

    protected void createChannel() {

        try {
            mChannel = this.mConnection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract ShutdownListener createShutDownListener();

    protected void setConnectionTimeOut(int requestTimeOut) {
        this.requestTimeOut = requestTimeOut;
    }

    protected void setRequestedHeartbeat(int requestHeartBeat) {
        this.requestHeartBeat = requestHeartBeat;
    }

    public boolean isConnected() {
        if (mConnection != null && mConnection.isOpen()) {
            return true;
        }
        return false;
    }

    public boolean isChannelAvailable() {
        if (mChannel != null && mChannel.isOpen()) {
            return true;
        }
        return false;
    }


}
