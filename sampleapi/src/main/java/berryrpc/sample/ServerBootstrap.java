package berryrpc.sample;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerBootstrap {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("rpc-server.xml");
    }
}
