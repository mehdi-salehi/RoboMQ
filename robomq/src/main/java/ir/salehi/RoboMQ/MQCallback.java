package ir.salehi.RoboMQ;

public interface MQCallback {
    void onMQConnectionFailure(String message);

    void onMQDisconnected();

    void onMQConnectionClosed(String message);
}
