package ir.salehi.mqsample;


public class MQConfig {
    public static final int port = 5672;
    public static final String exchange = "amq.direct";
    public static final String rotuingkey = "test";
    public static String hostName = "amqp://username:password@127.0.0.1/vhost";
}
