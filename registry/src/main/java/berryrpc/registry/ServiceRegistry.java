package berryrpc.registry;

public interface ServiceRegistry {

    void register(String interfaceName, String serverHost);
}
