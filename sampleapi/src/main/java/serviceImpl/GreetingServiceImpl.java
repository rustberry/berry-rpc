package serviceImpl;

import berryrpc.server.RpcService;
import service.GreetingService;

@RpcService(GreetingService.class)
public class GreetingServiceImpl implements GreetingService {

    @Override
    public String greet(String name) {
        long currentTime = System.currentTimeMillis();
        return "Hello, " + name + ", now it is: " + currentTime;
    }
}
