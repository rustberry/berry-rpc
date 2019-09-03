package berryrpc.registry;

public interface ServiceDiscovery {
    String discover(String serviceInterfaceName);
}
