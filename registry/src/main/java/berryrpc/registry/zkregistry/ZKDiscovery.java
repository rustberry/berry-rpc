package berryrpc.registry.zkregistry;

import berryrpc.registry.LoadBalancePolicy;
import berryrpc.registry.RandomLoadBalancePolicy;
import berryrpc.registry.ServiceDiscovery;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.WatchMode;
import org.apache.zookeeper.WatchedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Slf4j
public class ZKDiscovery extends AbstractZooKeeperClient implements ServiceDiscovery {
    private String registryHost;
    final private String registryPath = Constant.REGISTRY_PATH;

    private Map<String, List<String>> serviceProviderMap = new HashMap<>();

    private PathChildrenCache serviceCache;
    private AsyncCuratorFramework client;


    @Setter @Getter
    private LoadBalancePolicy policy;

    public ZKDiscovery(CuratorFramework client) {
        this.client = AsyncCuratorFramework.wrap(client);
    }

    public ZKDiscovery(String registryHost) {
        this.registryHost = registryHost;
        CuratorFramework synchClient = CuratorFrameworkFactory
                .newClient(registryHost, new ExponentialBackoffRetry(1000, 3));
        log.debug("berry-rpc: syncClient begin to start()");
        synchClient.start();
        log.debug("berry-rpc: syncClient finished start()");
        client = AsyncCuratorFramework.wrap(synchClient);
//        client = AsyncCuratorFramework.wrap(CuratorFrameworkFactory
//                .newClient(registryHost, new ExponentialBackoffRetry(1000, 3)));
        init(client);
    }

    private void init(AsyncCuratorFramework client) {
        check(client);
        retrieveInitServices(client);
        watchForChildAdded(client);
    }

    private void check(AsyncCuratorFramework client) {
        client.checkExists().forPath(this.registryPath)
                .thenAccept(stat -> {
                    if (stat == null) throw new RuntimeException("Registry path not created");
                });
    }

    private void retrieveInitServices(AsyncCuratorFramework client) {
        client.getChildren().forPath(this.registryPath)
                .thenAccept(strings -> {
                    if (strings != null) {
                        log.debug("Initial services are: " + strings);
                        for (String s : strings) {
                            serviceProviderMap.put(s, new ArrayList<>());
                            // Set the hosts providing this service.
                            setHosts(client, s);
                        }
                    } else {
                        // retry
                        log.warn("No initial services found under registryPath " + this.registryPath);
                        // successOnly ignores unnecessary notifications
                        retryOnceOnChildrenChanged(client.with(WatchMode.successOnly).watched()
                                .getChildren().forPath(registryPath).event(),
                                this::retrieveInitServices);
                    }
                });
    }

    private void retryOnceOnChildrenChanged(CompletionStage<WatchedEvent> watchedStage,
                                       Consumer<AsyncCuratorFramework> action) {
        watchedStage.thenAccept(watchedEvent -> {
            switch (watchedEvent.getType()) {
                case NodeChildrenChanged:
                    action.accept(this.client);
                    break;
            }
        });
    }

    /**
     * Set a watch on registryPath for {@code CHILD_ADDED} event.
     * On that event, update the {@code serviceProviderMap}.
     */
    private void watchForChildAdded(AsyncCuratorFramework client) {
        serviceCache = new PathChildrenCache(client.unwrap(), this.registryPath, false);
        // Listener for CHILD_ADDED event
        PathChildrenCacheListener listener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()) {
                    case CHILD_ADDED:
                        update();
                }
            }
        };

        try {
            serviceCache.getListenable().addListener(listener);
            serviceCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized private void update() {
        client.getChildren()
                .forPath(this.registryPath)
                .thenAccept(strings -> {
                    for (String s : strings) {
                        if (!serviceProviderMap.containsKey(s)) {
                            serviceProviderMap.put(s, new ArrayList<String>());
                            setHosts(client, s);
                        }
                    }
                });
    }

    private void setHosts(AsyncCuratorFramework client, String parentPathName) {
        client.getChildren()
                .forPath(this.registryPath + "/" + parentPathName)
                .thenAccept(strings -> {
                    serviceProviderMap.get(parentPathName).addAll(strings);
                });
    }

    synchronized private void updateSynchronously() throws Exception {
        List<String> children = client.unwrap().getChildren().forPath(this.registryPath);
        for (String s : children) {
            if (!serviceProviderMap.containsKey(s)) {
                // Put in interfaces
                serviceProviderMap.put(s, new ArrayList<>());
                // Put in hosts that provide interface services
                List<String> hosts = client.unwrap().getChildren().forPath(this.registryPath + "/" + s);
                serviceProviderMap.get(s).addAll(hosts);
            }
        }
    }

    @Override
    public String discover(String serviceInterfaceName) {
        List<String> providers = null;
        if (!serviceProviderMap.containsKey(serviceInterfaceName)) {
            // Maybe a new service was published, so query for child again.
            // To reduce load/reliance on ZooKeeper, I chose not set watch to keep up-to-date.
            try {
                updateSynchronously();
                providers = serviceProviderMap.get(serviceInterfaceName);
            } catch (Exception e) {
                log.error("Exception getting children from path " + this.registryPath);
                log.error("providers: " + providers);
                throw new RuntimeException("Exception getting children from path " + this.registryPath, e);
            }
            if (providers == null) throw new RuntimeException("No such service: " + serviceInterfaceName + " found");
        }

         providers = serviceProviderMap.get(serviceInterfaceName);

        if (providers.size() == 0) {
            log.error("serviceProviderMap: " + serviceProviderMap.toString());
            throw new RuntimeException("Service: " + serviceInterfaceName + " may not have been published, zero hosts found.");
        }

        if (providers.size() == 1) {
            return providers.get(0);
        } else {
            int designatedProvider = new RandomLoadBalancePolicy().select(providers);
            return providers.get(designatedProvider);
        }
    }

    @Override
    public void shutdown() {
        this.client.unwrap().close();
        CloseableUtils.closeQuietly(serviceCache);
    }
}
