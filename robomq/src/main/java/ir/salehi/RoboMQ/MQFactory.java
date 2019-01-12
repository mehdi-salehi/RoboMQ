package ir.salehi.RoboMQ;


public class MQFactory {
    private static final String TAG = "MQFactory";

    private String excahnge;
    private String routingKey;
    private String hostName;
    private int port;

    public MQFactory(String hostName, String excahnge, String routingKey, int port) {
        this.excahnge = excahnge;
        this.routingKey = routingKey;
        this.hostName = hostName;
        this.port = port;
    }


    public MQConsumer createConsumer(MQCallback callback) {
        MQConsumer consumer = MQConsumer.createInstance(this, callback);
        return consumer;
    }

    public MQProducer createProducer(MQCallback callback) {
        MQProducer producer = MQProducer.createInstance(this, callback);
        return producer;
    }

    public String getExcahnge() {
        return excahnge;
    }

    public void setExcahnge(String excahnge) {
        this.excahnge = excahnge;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
