package berryrpc.sample.serviceImpl;

import berryrpc.client.RpcProxy;
import berryrpc.sample.service.GreetingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Slf4j
public class GreetingClient {
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("rpc-client.xml");
        RpcProxy proxy = ctx.getBean(RpcProxy.class);
        GreetingService greetingService = proxy.create(GreetingService.class);
        String name1 = "GreetingNum1";
        String result = greetingService.greet(name1);
        log.debug("Greeting using name: " + name1 + ". Got: " + result);

        String name2 = "Greeting2";
        String result2 = greetingService.greet(name2);
        log.debug("Greeting using name: " + name2 + ". Got: " + result);

        proxy.shutdown();
        System.exit(0);
    }
}
